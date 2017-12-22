package oglutils;

import java.nio.Buffer;
import java.util.Arrays;
import java.util.List;

import com.jogamp.opengl.GL2GL3;

public class OGLRenderTarget {
	protected final GL2GL3 gl;
	protected final int width, height, count;
	protected final int[] drawBuffers;
	protected final int[] frameBuffer = new int[1];
	protected final OGLTexture2D[] colorBuffers;
	protected final OGLTexture2D depthBuffer;
	
	public OGLRenderTarget(GL2GL3 gl, int width, int height) {
		this(gl, width, height, 1);
	}

	public OGLRenderTarget(GL2GL3  gl, int width, int height, int count) {
		this(gl, width, height, count, new OGLTexImageFloat.Format(4));
	}

	public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> OGLRenderTarget(GL2GL3 gl, int width, int height,
			int count, OGLTexImage.Format<OGLTexImageType> format) {
		this(gl, width, height, count, null, format);
	}

	public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> OGLRenderTarget(GL2GL3 gl, int count,
			OGLTexImageType texImage) {
		this(gl, texImage.getWidth(), texImage.getHeight(), count, Arrays.asList(texImage), texImage.getFormat());
	}

	public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> OGLRenderTarget(GL2GL3 gl, OGLTexImageType[] texImage) {
		this(gl, texImage[0].getWidth(), texImage[0].getHeight(), texImage.length, Arrays.asList(texImage),
				texImage[0].getFormat());
	}

	public <OGLTexImageType extends OGLTexImage<OGLTexImageType>> OGLRenderTarget(GL2GL3 gl,
			List<OGLTexImageType> texImage) {
		this(gl, texImage.get(0).getWidth(), texImage.get(0).getHeight(), texImage.size(), texImage,
				texImage.get(0).getFormat());
	}

	private <OGLTexImageType extends OGLTexImage<OGLTexImageType>> OGLRenderTarget(GL2GL3 gl, int width, int height,
			int count, List<OGLTexImageType> texImage, OGLTexImage.Format<OGLTexImageType> format) {
		this.gl = gl;
		this.width = width;
		this.height = height;
		this.count = count;
		this.colorBuffers = new OGLTexture2D[count];
		this.drawBuffers = new int[count];
		for (int i = 0; i < count; i++) {
			Buffer imageData = texImage == null ? null : texImage.get(i).getDataBuffer();
			colorBuffers[i] = new OGLTexture2D(gl, width, height, 
					format.getInternalFormat(), format.getPixelFormat(), format.getPixelType(), imageData);
			drawBuffers[i] = GL2GL3.GL_COLOR_ATTACHMENT0 + i;
		}
		this.depthBuffer = new OGLTexture2D(gl, width, height, 
				GL2GL3.GL_DEPTH_COMPONENT, GL2GL3.GL_DEPTH_COMPONENT,
				GL2GL3.GL_FLOAT, null);
		
		gl.glGenFramebuffers(1, frameBuffer, 0);
		gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, frameBuffer[0]);
		for (int i = 0; i < count; i++)
			gl.glFramebufferTexture2D(GL2GL3.GL_FRAMEBUFFER, GL2GL3.GL_COLOR_ATTACHMENT0 + i, GL2GL3.GL_TEXTURE_2D,
					colorBuffers[i].getTextureId(), 0);
		gl.glFramebufferTexture2D(GL2GL3.GL_FRAMEBUFFER, GL2GL3.GL_DEPTH_ATTACHMENT, GL2GL3.GL_TEXTURE_2D,
				depthBuffer.getTextureId(), 0);

		if (gl.glCheckFramebufferStatus(GL2GL3.GL_FRAMEBUFFER) != GL2GL3.GL_FRAMEBUFFER_COMPLETE) {
			System.out.println("There is a problem with the FBO");
		}
	}

	public void bind() {
		gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, frameBuffer[0]);
		gl.glDrawBuffers(count, drawBuffers, 0);
		gl.glViewport(0, 0, width, height);
	}

	public void bindColorTexture(int shaderProgram, String name, int slot) {
		bindColorTexture(shaderProgram, name, slot, 0);
	}

	public void bindColorTexture(int shaderProgram, String name, int slot, int bufferIndex) {
		colorBuffers[bufferIndex].bind(shaderProgram, name, slot);
	}

	public void bindDepthTexture(int shaderProgram, String name, int slot) {
		depthBuffer.bind(shaderProgram, name, slot);
	}

	public OGLTexture2D getColorTexture() {
		return getColorTexture(0);
	}

	public OGLTexture2D getColorTexture(int bufferIndex) {
		return colorBuffers[bufferIndex];
	}

	public OGLTexture2D getDepthTexture() {
		return depthBuffer;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	@Override
	public void finalize() throws Throwable {
		super.finalize();
		gl.glDeleteFramebuffers(1, frameBuffer, 0);
	}

}
