package oglutils;


import java.nio.Buffer;

import com.jogamp.opengl.GL2GL3;

import transforms.Mat4Scale;
import transforms.Mat4Transl;

public class OGLTextureVolume implements OGLTexture {
	private final GL2GL3 gl;
	private final int[] volumeTextureID = new int[1];
	private final int width, height, depth;
	
	public static class Viewer extends OGLTexture2D.Viewer {
		private static final String shaderVertSrc[] = {
				"#version 330\n",
				"in vec2 inPosition;", 
				"in vec2 inTexCoord;", 
				"uniform mat4 matTrans;",
				"out vec2 texCoord;", 
				"void main() {",
				"	gl_Position = matTrans * vec4(inPosition , 0.0f, 1.0f);",
				"   texCoord = inTexCoord;",
				"}"
			};
		
		private static final String shaderFragSrc[] = { 
				"#version 330\n",
				"in vec2 texCoord;", 
				"out vec4 fragColor;", 
				"uniform sampler3D drawTexture;",
				"void main() {",
				"	vec3 coord;", 
				"	int row = 4;",
				"	int column = 4;",
				"	int i = int(texCoord.x * column);",
				"	int j = int(texCoord.y * row);",
				"	coord.x = (texCoord.x * column) - i;",
				"	coord.y = (texCoord.y * row) - j;",
				"	coord.z = (i + j*column)/float(row*column);",
				"	//fragColor = vec4( coord.xyz, 1.0);",
				"	fragColor = texture(drawTexture, coord);",
				"}" 
			};

		public Viewer(GL2GL3 gl) {
			super(gl, ShaderUtils.loadProgram(gl, shaderVertSrc, shaderFragSrc, null, null, null, null ));
		}

		@Override
		public void view(int textureID, double x, double y, double scale, double aspectXY, int level) {
			if (shaderProgram > 0) {
				gl.glUseProgram(shaderProgram);
				gl.glActiveTexture(GL2GL3.GL_TEXTURE0);
				gl.glEnable(GL2GL3.GL_TEXTURE_3D);
				gl.glUniformMatrix4fv(locMat, 1, false, ToFloatArray
						.convert(new Mat4Scale(scale * aspectXY, scale, 1).mul(new Mat4Transl(x, y, 0))), 0);
				gl.glUniform1i(locLevel, level);
				gl.glBindTexture(GL2GL3.GL_TEXTURE_3D, textureID);
				gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "drawTexture"), 0);
				buffers.draw(GL2GL3.GL_TRIANGLE_STRIP, shaderProgram);
				gl.glDisable(GL2GL3.GL_TEXTURE_3D);
				gl.glUseProgram(0);
			}
		}

	}

	public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> OGLTextureVolume(GL2GL3  gl, OGLTexImageType volume) {
		this.gl = gl;
		this.width = volume.getWidth();
		this.height = volume.getHeight();
		this.depth = volume.getDepth();
		Buffer buffer = volume.getDataBuffer();
		gl.glGenTextures(1, volumeTextureID, 0);
		gl.glBindTexture(GL2GL3.GL_TEXTURE_3D, volumeTextureID[0]);
		gl.glPixelStorei(GL2GL3.GL_UNPACK_ALIGNMENT, 1);

		gl.glTexImage3D(GL2GL3.GL_TEXTURE_3D, 0, 
				volume.getFormat().getInternalFormat(),
				volume.getWidth(), volume.getHeight(), volume.getDepth(),
				0, volume.getFormat().getPixelFormat(),
				volume.getFormat().getPixelType(), buffer);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_3D, GL2GL3.GL_TEXTURE_MAG_FILTER, GL2GL3.GL_LINEAR);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_3D, GL2GL3.GL_TEXTURE_MIN_FILTER, GL2GL3.GL_LINEAR);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_3D, GL2GL3.GL_TEXTURE_WRAP_S, GL2GL3.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_3D, GL2GL3.GL_TEXTURE_WRAP_T, GL2GL3.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_3D, GL2GL3.GL_TEXTURE_WRAP_R, GL2GL3.GL_CLAMP_TO_EDGE);
	}


	public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> void setTextureBuffer(
			OGLTexImage.Format<OGLTexImageType> format, Buffer buffer) {
		gl.glBindTexture(GL2GL3.GL_TEXTURE_3D, volumeTextureID[0]);
		gl.glTexSubImage3D(GL2GL3.GL_TEXTURE_3D, 0, 0, 0, 0, 
				width, height, depth, 
				format.getPixelFormat(), format.getPixelType(), buffer);
	}

	public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> Buffer getTextureBuffer(
			OGLTexImage.Format<OGLTexImageType> format) {
		gl.glBindTexture(GL2GL3.GL_TEXTURE_3D, volumeTextureID[0]);
		Buffer buffer = format.newBuffer(width, height, depth);
		gl.glGetTexImage(GL2GL3.GL_TEXTURE_3D, 0, format.getPixelFormat(), format.getPixelType(), buffer);
		return buffer;
	}

	public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> void setTexImage(OGLTexImageType volume) {
		setTextureBuffer(volume.getFormat(), volume.getDataBuffer());
	}

	public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> OGLTexImageType getTexImage(
			OGLTexImage.Format<OGLTexImageType> format) {
		OGLTexImageType image = format.newTexImage(width, height, depth);
		image.setDataBuffer(getTextureBuffer(format));
		return image;
	}
	
	@Override
	public void bind(int shaderProgram, String name, int slot) {
		gl.glActiveTexture(GL2GL3.GL_TEXTURE0 + slot);
		gl.glBindTexture(GL2GL3.GL_TEXTURE_3D, volumeTextureID[0]);
		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, name), slot);
	}
	
	@Override
	public void bind(int shaderProgram, String name) {
		bind(shaderProgram, name, 0);
	}
	
	@Override
	public int getTextureId(){
		return volumeTextureID[0]; 
	}
	
	@Override
	public void finalize() throws Throwable {
		super.finalize();
		gl.glDeleteTextures(1, volumeTextureID, 0);
	}
}
