package oglutils;


import transforms.Mat4Scale;
import transforms.Mat4Transl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

public class OGLTexture2D implements OGLTexture {
	private final GL2GL3 gl;
	private final int[] textureID = new int[1];
	private final int width, height;
	
	public static class Viewer implements OGLTexture.Viewer {
		protected final GL2GL3 gl;
		protected final int shaderProgram;
		protected final OGLBuffers buffers;
		protected final int locMat;
		protected final int locLevel;
		
		private static final String[] SHADER_VERT_SRC = {
				"#version 330\n",
				"in vec2 inPosition;", 
				"in vec2 inTexCoord;", 
				"uniform mat4 matTrans;",
				"out vec2 texCoords;", 
				"void main() {",
				"	gl_Position = matTrans * vec4(inPosition , 0.0f, 1.0f);",
				"   texCoords = inTexCoord;",
				"}"
			};
		
		private static final String[] SHADER_FRAG_SRC = { 
				"#version 330\n",
				"in vec2 texCoords;", 
				"out vec4 fragColor;", 
				"uniform sampler2D drawTexture;",
				"uniform int level;",
				"void main() {",
				" 	fragColor = texture(drawTexture, texCoords);", 
				" 	if (level >= 0)", 
				" 		fragColor = textureLod(drawTexture, texCoords, level);", 
				"}" 
			};

		private OGLBuffers createBuffers(GL2GL3 gl) {
			float[] vertexBufferData = { 
					0, 0, 0, 0, 
					1, 0, 1, 0, 
					0, 1, 0, 1,
					1, 1, 1, 1 };
			int[] indexBufferData = { 0, 1, 2, 3 };

			OGLBuffers.Attrib[] attributes = { new OGLBuffers.Attrib("inPosition", 2),
					new OGLBuffers.Attrib("inTexCoord", 2) };

			return new OGLBuffers(gl, vertexBufferData, attributes, indexBufferData);
		}

		public Viewer(GL2GL3 gl) {
			this(gl, ShaderUtils.loadProgram(gl, SHADER_VERT_SRC, SHADER_FRAG_SRC, null, null, null, null));
		}
		
		protected Viewer(GL2GL3 gl, int shaderProgram) {
			this.gl = gl;
			buffers = createBuffers(gl);
			this.shaderProgram = shaderProgram; 
			locMat = this.gl.glGetUniformLocation(shaderProgram, "matTrans");
			locLevel = this.gl.glGetUniformLocation(shaderProgram, "level");
		}

		@Override
		public void view(int textureID) {
			view(textureID, -1, -1);
		}

		@Override
		public void view(int textureID, double x, double y) {
			view(textureID, x, y, 1.0, 1.0);
		}

		@Override
		public void view(int textureID, double x, double y, double scale) {
			view(textureID, x, y, scale, 1.0);
		}
		@Override
		public void view(int textureID, double x, double y, double scale, double aspectXY) {
			view(textureID, x, y, scale, aspectXY, -1);
		}	
		
		@Override
		public void view(int textureID, double x, double y, double scale, double aspectXY, int level) {
			if (shaderProgram > 0) {
				gl.glUseProgram(shaderProgram);
				gl.glActiveTexture(GL2GL3.GL_TEXTURE0);
				gl.glEnable(GL2GL3.GL_TEXTURE_2D);
				gl.glUniformMatrix4fv(locMat, 1, false, ToFloatArray
						.convert(new Mat4Scale(scale * aspectXY, scale, 1).mul(new Mat4Transl(x, y, 0))), 0);
				gl.glUniform1i(locLevel, level);
				gl.glBindTexture(GL2GL3.GL_TEXTURE_2D, textureID);
				gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "drawTexture"), 0);
				buffers.draw(GL2GL3.GL_TRIANGLE_STRIP, shaderProgram);
				gl.glDisable(GL2GL3.GL_TEXTURE_2D);
				gl.glUseProgram(0);
			}
		}
		
		@Override
		public void finalize() throws Throwable {
			super.finalize();
			gl.glDeleteProgram(shaderProgram);
		}

	}
		
	private static String getExtension(String s) {
		String ext = "";
		int i = s.lastIndexOf('.');
		if (i > 0 && i < s.length() - 1) {
			ext = s.substring(i + 1).toLowerCase();
		}
		return ext;
	}
	
	static TextureData readTextureDataFromFile(GLProfile glProfile, String fileName) {
			System.out.print("Reading texture file " + fileName);
			try {
				InputStream is = OGLTexture2D.class.getResourceAsStream(fileName);
				//there are some problems on Mac OS with mipmap, in this case set false
				TextureData data = TextureIO.newTextureData(glProfile, is, true,
						getExtension(fileName));
				is.close();
				System.out.println(" ... OK");
				return data;
			} catch (IOException e) {
				System.err.println(" failed");
				throw new RuntimeException(e);
			}
	}

	public OGLTexture2D(GL2GL3 gl, int width, int height, int textureId) {
		this.gl = gl;
		this.width = width;
		this.height = height;
		this.textureID[0] = textureId;
	}

	public OGLTexture2D(GL2GL3 gl, int width, int height, int internalFormat, int pixelFormat, int pixelType, Buffer buffer) {
		this.gl = gl;
		this.width = width;
		this.height = height;
		gl.glGenTextures(1, textureID, 0);
		gl.glBindTexture(GL2GL3.GL_TEXTURE_2D, textureID[0]);
		gl.glTexImage2D(GL2GL3.GL_TEXTURE_2D, 0, internalFormat, 
				width, height, 0, 
				pixelFormat, pixelType, buffer);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_WRAP_S, GL2GL3.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_WRAP_T, GL2GL3.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MIN_FILTER, GL2GL3.GL_LINEAR);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MAG_FILTER, GL2GL3.GL_LINEAR);
	}
	
	public OGLTexture2D(GL2GL3 gl, TextureData textureData) {
		this(gl, textureData.getWidth(),	textureData.getHeight(), 
				textureData.getInternalFormat(), textureData.getPixelFormat(), 
				textureData.getPixelType(), textureData.getBuffer());
	}
	
	public OGLTexture2D(GL2GL3 gl, String fileName) {
		this(gl, readTextureDataFromFile(gl.getGLProfile(), fileName));
	}
	
	
	public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> OGLTexture2D(GL2GL3 gl, OGLTexImageType image) {
		this(gl, image.getWidth(),	image.getHeight(), 
				image.getFormat().getInternalFormat(), image.getFormat().getPixelFormat(), 
				image.getFormat().getPixelType(), image.getDataBuffer());
	}
	
	public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> void setTextureBuffer(
			OGLTexImage.Format<OGLTexImageType> format, Buffer buffer) {
		bind();
		gl.glTexSubImage2D(GL2GL3.GL_TEXTURE_2D, 0, 0, 0, getWidth(), getHeight(), 
				format.getPixelFormat(), format.getPixelType(), buffer);
	}

	public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> Buffer getTextureBuffer(
			OGLTexImage.Format<OGLTexImageType> format) {
		bind();
		Buffer buffer = format.newBuffer(getWidth(), getHeight());
		gl.glGetTexImage(GL2GL3.GL_TEXTURE_2D, 0, format.getPixelFormat(), format.getPixelType(), buffer);
		return buffer;
	}

	public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> void setTextureBuffer(
			OGLTexImage.Format<OGLTexImageType> format, Buffer buffer, int level) {
		bind();
		gl.glTexSubImage2D(GL2GL3.GL_TEXTURE_2D, level, 0, 0, 
				getWidth() >> level, getHeight() >> level, 
				format.getPixelFormat(), format.getPixelType(), buffer);
	}

	public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> Buffer getTextureBuffer(
			OGLTexImage.Format<OGLTexImageType> format, int level) {
		bind();
		Buffer buffer = format.newBuffer(getWidth() >> level, getHeight() >> level);
		gl.glGetTexImage(GL2GL3.GL_TEXTURE_2D, level, format.getPixelFormat(), format.getPixelType(), buffer);
		return buffer;
	}

	public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> void setTexImage(OGLTexImageType image) {
		setTextureBuffer(image.getFormat(), image.getDataBuffer());
	}

	public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> OGLTexImageType getTexImage(
			OGLTexImage.Format<OGLTexImageType> format) {
		OGLTexImageType image = format.newTexImage(getWidth(), getHeight());
		image.setDataBuffer(getTextureBuffer(format));
		return image;
	}

	public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> void setTexImage(OGLTexImageType image, int level) {
		setTextureBuffer(image.getFormat(), image.getDataBuffer(), level);
	}

	public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> OGLTexImageType getTexImage(
			OGLTexImage.Format<OGLTexImageType> format, int level) {
		OGLTexImageType image = format.newTexImage(getWidth() >> level, getHeight() >> level);
		image.setDataBuffer(getTextureBuffer(format, level));
		return image;
	}

	public void bind() {
		gl.glBindTexture(GL2GL3.GL_TEXTURE_2D, textureID[0]);
	}

	@Override
	public void bind(int shaderProgram, String name, int slot) {
		gl.glActiveTexture(GL2GL3.GL_TEXTURE0 + slot);
		bind();
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

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public BufferedImage toBufferedImage() {
		int[] array = new int[getWidth() * getHeight()];
		bind();
		gl.glGetTexImage(GL2GL3.GL_TEXTURE_2D, 0, GL2GL3.GL_RGBA, GL2GL3.GL_UNSIGNED_BYTE, IntBuffer.wrap(array));
		BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, getWidth(), getHeight(), array, 0, getWidth());
		return image;
	}

	public void fromBufferedImage(BufferedImage img) {
		bind();
		int[] array = new int[getWidth() * getHeight()];
		img.getRGB(0, 0, getWidth(), getHeight(), array, 0, getWidth());
		gl.glTexSubImage2D(GL2GL3.GL_TEXTURE_2D, 0, 0, 0, getWidth(), getHeight(), GL2GL3.GL_RGBA,
				GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV, IntBuffer.wrap(array));
	}

	@Override
	public void finalize() throws Throwable{
		super.finalize();
		gl.glDeleteTextures(1, textureID, 0);
	}
	

}
