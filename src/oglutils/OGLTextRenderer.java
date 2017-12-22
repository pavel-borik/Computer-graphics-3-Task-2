package oglutils;

import java.awt.Color;
import java.awt.Font;

import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.util.awt.TextRenderer;

public class OGLTextRenderer {
	private final GL2GL3 gl;
	private final TextRenderer renderer;
	private int width;
	private int height;
	private Color color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
	
	
	/**
	 * Create TextRenderer object
	 * 
	 * @param width
	 *            width of output rendering frame
	 * @param height
	 *            height of output rendering frame
	 * @param font
	 * 			  font
	 */
	public OGLTextRenderer(GL2GL3 gl, int width, int height, Font font) {
		this.gl = gl;
		this.width = width;
		this.height = height;
		renderer = new TextRenderer(font);
	}

	/**
	 * Create TextRenderer object
	 * 
	 * @param width
	 *            width of output rendering frame
	 * @param height
	 *            height of output rendering frame
	 */
	public OGLTextRenderer(GL2GL3 gl, int width, int height) {
		this(gl, width, height, new Font("SansSerif", Font.PLAIN, 12));
	}

	/**
	 * Update size of output rendering frame
	 * 
	 * @param width
	 *            updated width of output rendering frame
	 * @param height
	 *            updated height of output rendering frame
	 */
	public void updateSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	/**
	 * Changes the current color. The default color is opaque white.
	 * 
	 * @param color
	 *            the new color to use for rendering text
	 */
	public void setColor(Color color) {
		this.color = color;
	}
	 
	/**
	 * Draw string on 2D coordinates of the raster frame
	 * 
	 * @param gl
	 * @param x
	 *            x position of string in range <0, width-1> of raster frame
	 * @param y
	 *            y position of string in range <0, height-1> of raster frame
	 * @param s
	 *            string to draw
	 */
	public void drawStr2D(int x, int y,	String s) {
		if (renderer == null || s == null)
			return;
		gl.glUseProgram(0);
		gl.glViewport(0, 0, width, height);
		gl.glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
		gl.glActiveTexture(GL2GL3.GL_TEXTURE0);
		
		renderer.setColor(color);

		renderer.beginRendering(width, height);
		renderer.draw(s, x, y);
		
		renderer.endRendering();
	}

	@Override
	public void finalize() throws Throwable{
		super.finalize();
		renderer.dispose();
	}
}
