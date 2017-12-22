package oglutils;


import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.GL2GL3;

import java.nio.Buffer;

import transforms.Mat4Scale;
import transforms.Mat4Transl;

public class OGLTextureCube implements OGLTexture {
	
	private final GL2GL3 gl;
	private final int[] textureID = new int[1];
	
	private class TargetSize {
		private final int width, height;
		public TargetSize(int width, int height) {
			this.width = width; this.height = height;
		}
		public int getWidth() {
			return width;
		}
		public int getHeight() {
			return height;
		}

	}
	
	private final TargetSize[] targetSize;
	public static final String[] SUFFICES_POS_NEG = { "posx", "negx", "posy", "negy", "posz", "negz" };
	public static final String[] SUFFICES_POS_NEG_FLIP_Y = { "posx", "negx", "negy", "posy", "posz", "negz" };
	public static final String[] SUFFICES_POSITIVE_NEGATIVE = { "positive_x", "negative_x", "positive_y", "negative_y", "positive_z", "negative_z" };
	public static final String[] SUFFICES_POSITIVE_NEGATIVE_FLIP_Y = { "positive_x", "negative_x", "negative_y", "positive_y", "positive_z", "negative_z" };
	public static final String[] SUFFICES_RIGHT_LEFT = { "right", "left", "bottom", "top", "front", "back" };
	public static final String[] SUFFICES_RIGHT_LEFT_FLIP_Y  = { "right", "left", "top", "bottom", "front", "back" };
	private static final int[] TARGETS = { GL2GL3.GL_TEXTURE_CUBE_MAP_POSITIVE_X,
	                                         GL2GL3.GL_TEXTURE_CUBE_MAP_NEGATIVE_X,
	                                         GL2GL3.GL_TEXTURE_CUBE_MAP_POSITIVE_Y,
	                                         GL2GL3.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y,
	                                         GL2GL3.GL_TEXTURE_CUBE_MAP_POSITIVE_Z,
	                                         GL2GL3.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z };
	
	public static class Viewer extends OGLTexture2D.Viewer {
		private static final String[]SHADER_VERT_SRC = {
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
		
		private static final String[] SHADER_FRAG_SRC = { 
				"#version 330\n",
				"in vec2 texCoord;", 
				"out vec4 fragColor;", 
				"uniform samplerCube drawTexture;",
				"void main() {",
				" 	//fragColor = vec4( texCoord.xy, 0.0, 1.0);", 
				"	vec2 coord;", 
				//top
				"	if ((texCoord.y <= 1.0) &&(texCoord.y >= 2.0/3.0) && (texCoord.x >= 1.0/4.0) && (texCoord.x <= 2.0/4.0)){", 
				"		coord.y = (texCoord.y - 2.0/3.0) * 3.0 * 2.0 - 1.0;", 
				"		coord.x = (texCoord.x - 1.0/4.0) * 4.0 * 2.0 - 1.0;", 
				"		fragColor = texture(drawTexture, vec3(coord.x, -1.0, -coord.y));", 
				"	}else", 
				"	if ((texCoord.y >= 0.0) &&(texCoord.y <= 1.0/3.0) && (texCoord.x >= 1.0/4.0) && (texCoord.x <= 2.0/4.0)){", 
				"		coord.y = (texCoord.y) * 3.0 * 2.0 - 1.0;", 
				"		coord.x = (texCoord.x - 1.0/4.0) * 4.0 * 2.0 - 1.0;", 
				"		fragColor = texture(drawTexture, vec3(coord.x, 1.0, coord.y));", 
				"	}else", 
				//front
				"	if ((texCoord.y <= 2.0/3.0) && (texCoord.y >= 1.0/3.0) && (texCoord.x >= 1.0/4.0) && (texCoord.x <= 2.0/4.0)){", 
				"		coord.y = (texCoord.y - 1.0/3.0) * 3.0 * 2.0 - 1.0;", 
				"		coord.x = (texCoord.x - 1.0/4.0) * 4.0 * 2.0 - 1.0;", 
				"		fragColor = texture(drawTexture, vec3( coord.x, -coord.y, +1.0));", 
				"	}else", 
				"	if ((texCoord.y <= 2.0/3.0) && (texCoord.y >= 1.0/3.0) && (texCoord.x >= 3.0/4.0) && (texCoord.x <= 4.0/4.0)){", 
				"		coord.y = (texCoord.y - 1.0/3.0) * 3.0 * 2.0 - 1.0;", 
				"		coord.x = (texCoord.x - 3.0/4.0) * 4.0 * 2.0 - 1.0;", 
				"		fragColor = texture(drawTexture, vec3( -coord.x, -coord.y, -1.0));", 
				"	}else", 
		   	    //left
				"	if ((texCoord.y <= 2.0/3) && (texCoord.y >= 1.0/3.0) && (texCoord.x >= 0.0) && (texCoord.x <= 1.0/4.0)){", 
				"		coord.y = (texCoord.y - 1.0/3.0) * 3.0 * 2.0 - 1.0;", 
				"		coord.x = (texCoord.x ) * 4.0 * 2.0 - 1.0;", 
				"		fragColor = texture(drawTexture, vec3( -1.0, -coord.y, coord.x));", 
				"	}else", 
				"	if ((texCoord.y <= 2.0/3.0) && (texCoord.y >= 1.0/3.0) && (texCoord.x >= 1.0/2.0) && (texCoord.x <= 3.0/4.0)){", 
				"		coord.y = (texCoord.y - 1.0/3) * 3.0 * 2.0 - 1.0;", 
				"		coord.x = (texCoord.x - 2.0/4) * 4.0 * 2.0 - 1.0;", 
				"		fragColor = texture(drawTexture, vec3( +1.0, -coord.y, -coord.x));", 
				"	} else", 
				"		discard;", 
				"}" 
			};
		public Viewer(GL2GL3 gl) {
			super(gl, ShaderUtils.loadProgram(gl, SHADER_VERT_SRC, SHADER_FRAG_SRC, null, null, null, null ));
		}

		@Override
		public void view(int textureID, double x, double y, double scale, double aspectXY, int level) {
			if (shaderProgram > 0) {
				gl.glUseProgram(shaderProgram);
				gl.glActiveTexture(GL2GL3.GL_TEXTURE0);
				gl.glEnable(GL2GL3.GL_TEXTURE_CUBE_MAP);
				gl.glUniformMatrix4fv(locMat, 1, false, ToFloatArray
						.convert(new Mat4Scale(scale * aspectXY, scale, 1).mul(new Mat4Transl(x, y, 0))), 0);
				gl.glUniform1i(locLevel, level);
				gl.glBindTexture(GL2GL3.GL_TEXTURE_CUBE_MAP, textureID);
				gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "drawTexture"), 0);
				buffers.draw(GL2GL3.GL_TRIANGLE_STRIP, shaderProgram);
				gl.glDisable(GL2GL3.GL_TEXTURE_CUBE_MAP);
				gl.glUseProgram(0);
			}
		}
	}
	
	private OGLTextureCube(GL2GL3 gl) {
		this.gl = gl;
		targetSize = new TargetSize[6];
		gl.glGenTextures(1, textureID, 0);
		bind();
	}
	private void setTarget(TextureData data, int target) {
		targetSize[target] = new TargetSize(data.getWidth(), data.getHeight());
		gl.glTexImage2D(TARGETS[target], 0, data.getInternalFormat(), 
				data.getWidth(), data.getHeight(), 0, 
				data.getPixelFormat(), data.getPixelType(), data.getBuffer());
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_CUBE_MAP, GL2GL3.GL_TEXTURE_WRAP_S, GL2GL3.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_CUBE_MAP, GL2GL3.GL_TEXTURE_WRAP_T, GL2GL3.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_CUBE_MAP, GL2GL3.GL_TEXTURE_WRAP_R, GL2GL3.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_CUBE_MAP, GL2GL3.GL_TEXTURE_MIN_FILTER, GL2GL3.GL_LINEAR);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_CUBE_MAP, GL2GL3.GL_TEXTURE_MAG_FILTER, GL2GL3.GL_LINEAR);

	}
	
	public OGLTextureCube(GL2GL3 gl, String[] fileNames) {
		this(gl);
		for (int i = 0; i < fileNames.length; i++) {
    		TextureData data;
			data = OGLTexture2D.readTextureDataFromFile(gl.getGLProfile(), fileNames[i]);
			setTarget(data, i);
		}
   }

	public OGLTextureCube(GL2GL3 gl, String fileName, String[] suffixes) {
		this(gl);
		String baseName=fileName.substring(0,fileName.lastIndexOf('.'));
    	String suffix=fileName.substring(fileName.lastIndexOf('.')+1,fileName.length());
    	for (int i = 0; i < suffixes.length; i++) {
    		String fullName = new String(baseName + suffixes[i] + "." + suffix);
    		TextureData data;
			data = OGLTexture2D.readTextureDataFromFile(gl.getGLProfile(), fullName);
			setTarget(data, i);
    	}
   }

	public void bind() {
		gl.glBindTexture(GL2GL3.GL_TEXTURE_CUBE_MAP, textureID[0]);
	}

	@Override
	public void bind(int shaderProgram, String name, int slot) {
		bind();
		gl.glActiveTexture(GL2GL3.GL_TEXTURE0 + slot);
		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, name), slot);
	}

	@Override
	public void bind(int shaderProgram, String name) {
		bind(shaderProgram, name, 0);
	}

	@Override
	public int getTextureId(){
		return textureID[0]; 
	}
	
	public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> void setTextureBuffer(
			OGLTexImage.Format<OGLTexImageType> format, Buffer buffer, int cubeFaceIndex) {
		bind();
		gl.glTexSubImage2D(TARGETS[cubeFaceIndex], 0, 0, 0, 
				targetSize[cubeFaceIndex].getWidth(),targetSize[cubeFaceIndex].getHeight(), 
				format.getPixelFormat(), format.getPixelType(), buffer);
	}

	public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> Buffer getTextureBuffer(
			OGLTexImage.Format<OGLTexImageType> format, int cubeFaceIndex) {
		bind();
		Buffer buffer = Buffers.newDirectByteBuffer(targetSize[cubeFaceIndex].getWidth() 
				* targetSize[cubeFaceIndex].getHeight() * 4);
		gl.glGetTexImage(TARGETS[cubeFaceIndex], 0, format.getPixelFormat(), format.getPixelType(), buffer);
		return buffer;
	}

	public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> void setTexImage(OGLTexImageType image, int cubeFaceIndex) {
		setTextureBuffer(image.getFormat(), image.getDataBuffer(), cubeFaceIndex);
	}

	public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> OGLTexImageType getTexImage(
			OGLTexImage.Format<OGLTexImageType> format, int cubeFaceIndex) {
		OGLTexImageType image = format.newTexImage(
				 targetSize[cubeFaceIndex].getWidth(),  targetSize[cubeFaceIndex].getHeight());
		image.setDataBuffer(getTextureBuffer(format, cubeFaceIndex));
		return image;
	}
	
	@Override
	public void finalize() throws Throwable {
		super.finalize();
		gl.glDeleteTextures(1, textureID, 0);
	}
}
