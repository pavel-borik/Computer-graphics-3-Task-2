package application;

import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import oglutils.*;
import transforms.*;

import javax.swing.*;
import java.awt.event.*;

public class Renderer implements GLEventListener, MouseListener,
		MouseMotionListener, KeyListener {

	OGLBuffers torus,mushroom, floor, sphere;
	OGLTextRenderer textRenderer;

	int width, height, ox, oy;
	int shaderProgram, shaderProgram2;
	int locObj,locModelMat,locViewMat,locProjMat,locEye,locBaseCol, locLightDir, locLightMatrix, locAAmode;
	int locProjMat2, locModelMat2, locViewMat2, locEye2, locObj2, locBaseCol2, locLightPos2;
	int objSwitch = 0, aaSwitch = 0;
	int polygonMode = GL2GL3.GL_FILL;
	OGLTexture2D textureDepth,textureColor;
	OGLTexture2D.Viewer textureViewer;
	OGLRenderTarget renderTarget;

	Mat4 lightViewMat, lightProjMat;
	boolean saved = false;

	Vec3D baseColor= new Vec3D(1,0,0);
	Vec3D lightDirection = new Vec3D(10.0, 0.0, 8.0);

	Camera cam = new Camera();
	Mat4 model, proj;

	@Override
	public void init(GLAutoDrawable glDrawable) {
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();
		OGLUtils.shaderCheck(gl);
		gl = OGLUtils.getDebugGL(gl);
		glDrawable.setGL(gl);
		OGLUtils.printOGLparameters(gl);
		
		textRenderer = new OGLTextRenderer(gl, glDrawable.getSurfaceWidth(), glDrawable.getSurfaceHeight());

		shaderProgram = ShaderUtils.loadProgram(gl, "/application/phong");
		shaderProgram2 = ShaderUtils.loadProgram(gl, "/application/phong");

		floor = MeshGenerator.generateGrid(gl, 40, 40,"inPosition");
		torus = MeshGenerator.generateGrid(gl, 40, 40,"inPosition");
		mushroom = MeshGenerator.generateGrid(gl, 40, 40,"inPosition");
		sphere = MeshGenerator.generateGrid(gl, 40, 40,"inPosition");

		locObj = gl.glGetUniformLocation(shaderProgram, "object");
		locModelMat = gl.glGetUniformLocation(shaderProgram, "modelMat");
		locViewMat = gl.glGetUniformLocation(shaderProgram, "viewMat");
		locProjMat = gl.glGetUniformLocation(shaderProgram, "projMat");
		locEye = gl.glGetUniformLocation(shaderProgram, "eyePos");
		locBaseCol = gl.glGetUniformLocation(shaderProgram, "baseCol");
		locLightDir = gl.glGetUniformLocation(shaderProgram, "lightDir");
		locLightMatrix = gl.glGetUniformLocation(shaderProgram, "lightMVP");
		locAAmode = gl.glGetUniformLocation(shaderProgram, "aaMode");

/*
		locObj2 = gl.glGetUniformLocation(shaderProgram2, "object");
		locModelMat2 = gl.glGetUniformLocation(shaderProgram2, "modelMat");
		locViewMat2 = gl.glGetUniformLocation(shaderProgram2, "viewMat");
		locProjMat2 = gl.glGetUniformLocation(shaderProgram2, "projMat");
		locEye2 = gl.glGetUniformLocation(shaderProgram2, "eyePos");
		locBaseCol2 = gl.glGetUniformLocation(shaderProgram2, "baseCol");
		locLightPos2 = gl.glGetUniformLocation(shaderProgram2, "lightPos");
*/
		renderTarget = new OGLRenderTarget(gl, 1024, 1024);

		cam = cam.withPosition(new Vec3D(25, 25, 5))
				.withAzimuth(Math.PI * 1.25)
				.withZenith(Math.PI * -0.05);

		lightViewMat =
				new Camera().withPosition(lightDirection.opposite())
				.withAzimuth(Math.PI * 1.00)
				.withZenith(Math.PI * -0.25).getViewMatrix();


		lightProjMat =  new Mat4OrthoRH(width/20, height/20 , 0.1, 100.0).mul(new Mat4Scale((double) width / height, 1, 1));

		model = new Mat4Identity();

		gl.glEnable(GL2GL3.GL_DEPTH_TEST);
		textureViewer = new OGLTexture2D.Viewer(gl);
	}

	@Override
	public void display(GLAutoDrawable glDrawable) {
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();

		gl.glUseProgram(shaderProgram);
		objSwitch = 0;
		renderTarget.bind();
		gl.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);
		gl.glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, polygonMode);

		// view and projection matrices of the light are used in the first run to calculate the shadows' position
		gl.glUniformMatrix4fv(locModelMat, 1, false,
				ToFloatArray.convert(model), 0);
		gl.glUniformMatrix4fv(locViewMat, 1, false,
				ToFloatArray.convert(lightViewMat), 0);
		gl.glUniformMatrix4fv(locProjMat, 1, false,
				ToFloatArray.convert( lightProjMat), 0);
		gl.glUniformMatrix4fv(locLightMatrix, 1, false, ToFloatArray.convert(model.mul(lightViewMat).mul(lightProjMat)),0);

//		gl.glUniform1i(locAAmode, aaSwitch);
//		gl.glUniform1i(locObj, objSwitch);
//		gl.glUniform3fv(locEye, 1, ToFloatArray.convert(cam.getEye()), 0);
//		//gl.glUniform3fv(locBaseCol, 1, ToFloatArray.convert(baseColor), 0);
//		gl.glUniform3fv(locLightDir, 1, ToFloatArray.convert(lightDirection), 0);


		floor.draw(GL2GL3.GL_TRIANGLES, shaderProgram);

		objSwitch = 3;
		gl.glUniform1i(locObj, objSwitch);
		sphere.draw(GL2GL3.GL_TRIANGLES, shaderProgram);

		objSwitch = 1;
		gl.glUniform1i(locObj, objSwitch);
		torus.draw(GL2GL3.GL_TRIANGLES, shaderProgram);

		objSwitch = 2;
		gl.glUniform1i(locObj, objSwitch);
		mushroom.draw(GL2GL3.GL_TRIANGLES, shaderProgram);

		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_COMPARE_MODE, GL2GL3.GL_NONE);
		textureColor = renderTarget.getDepthTexture();

		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_COMPARE_MODE, GL2GL3.GL_COMPARE_REF_TO_TEXTURE);
		textureDepth = renderTarget.getDepthTexture();

//----------------------------------2nd GPU run------------------------------------------//
		gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, 0);
		gl.glViewport(0, 0, width, height);

		baseColor = new Vec3D(0.5, 0.5, 0.5);
		objSwitch = 0;
		gl.glUseProgram(shaderProgram);
		//textureDepth.bind();
		gl.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);
		//renderTarget.getDepthTexture().bind(shaderProgram, "textureDepth", 0);

		gl.glUniformMatrix4fv(locModelMat, 1, false,
				ToFloatArray.convert(model), 0);
		gl.glUniformMatrix4fv(locViewMat, 1, false,
				ToFloatArray.convert(cam.getViewMatrix()), 0);
		gl.glUniformMatrix4fv(locProjMat, 1, false,
				ToFloatArray.convert(proj), 0);
		gl.glUniform1i(locObj, objSwitch);
		gl.glUniform3fv(locEye, 1, ToFloatArray.convert(cam.getEye()), 0);
		gl.glUniform3fv(locBaseCol, 1, ToFloatArray.convert(baseColor), 0);
		gl.glUniform3fv(locLightDir, 1, ToFloatArray.convert(lightDirection), 0);
		gl.glUniform1i(locAAmode, aaSwitch);

		gl.glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, polygonMode);

		floor.draw(GL2GL3.GL_TRIANGLES, shaderProgram);

		baseColor = new Vec3D(0.0, 1.0, 1.0);
		objSwitch = 3;
		gl.glUniform3fv(locBaseCol, 1, ToFloatArray.convert(baseColor), 0);
		gl.glUniform1i(locObj, objSwitch);
		sphere.draw(GL2GL3.GL_TRIANGLES, shaderProgram);

		baseColor = new Vec3D(1.0, 1.0, 0.0);
		objSwitch = 1;
		gl.glUniform1i(locObj, objSwitch);
		gl.glUniform3fv(locBaseCol, 1, ToFloatArray.convert(baseColor), 0);
		torus.draw(GL2GL3.GL_TRIANGLES, shaderProgram);

		objSwitch = 2;
		gl.glUniform1i(locObj, objSwitch);
		mushroom.draw(GL2GL3.GL_TRIANGLES, shaderProgram);

		gl.glUseProgram(0);
		//textureViewer.view(textureColor, -1, -0.5, 0.5, height / (double) width);
		//textureViewer.view(textureDepth, -1, -1, 0.5, height / (double) width);

		drawStrings();
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {
		this.width = width;
		this.height = height;

		proj = new Mat4PerspRH(Math.PI / 4, height / (double) width, 0.01, 1000.0);
		lightProjMat = new Mat4OrthoRH(width/15, height/15 , 0.1, 100.0).mul(new Mat4Scale((double) width / height, 1, 1));

		textRenderer.updateSize(width, height);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		ox = e.getX();
		oy = e.getY();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent e) {

		if(SwingUtilities.isLeftMouseButton(e)) {
			cam = cam.addAzimuth((double) Math.PI * (ox - e.getX()) / width)
					.addZenith((double) Math.PI * (e.getY() - oy) / width);

		}
		if(SwingUtilities.isRightMouseButton(e)) {
			model = model.mul(new Mat4RotZ((ox - e.getX())*0.02));
		}

		ox = e.getX();
		oy = e.getY();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
			case KeyEvent.VK_W:
				cam = cam.forward(0.5);
				break;
			case KeyEvent.VK_D:
				cam = cam.right(0.5);
				break;
			case KeyEvent.VK_S:
				cam = cam.backward(0.5);
				break;
			case KeyEvent.VK_A:
				cam = cam.left(0.5);
				break;
			case KeyEvent.VK_CONTROL:
				cam = cam.down(0.5);
				break;
			case KeyEvent.VK_SHIFT:
				cam = cam.up(0.5);
				break;
			case KeyEvent.VK_SPACE:
				cam = cam.withFirstPerson(!cam.getFirstPerson());
				break;
			case KeyEvent.VK_R:
				cam = cam.mulRadius(0.9f);
				break;
			case KeyEvent.VK_F:
				cam = cam.mulRadius(1.1f);
				break;
			case KeyEvent.VK_E:
				aaSwitch = (aaSwitch + 1) % 3;
				break;
			case KeyEvent.VK_O:
				model = model.mul(new Mat4RotX(0.08));
				break;
			case KeyEvent.VK_P:
				model = model.mul(new Mat4RotX(-0.08));
				break;
			case KeyEvent.VK_V:
				System.out.println(lightViewMat);
				break;
			case KeyEvent.VK_B:
				polygonMode = polygonMode == GL2GL3.GL_FILL ? GL2GL3.GL_LINE : GL2GL3.GL_FILL;
				break;
			case KeyEvent.VK_T:
				break;
			case KeyEvent.VK_L:
				break;
			case KeyEvent.VK_N:
				/*lightPos = lightPos.mul(new Mat3RotZ(0.1));
				lightViewMat = new Camera().withPosition(lightPos.opposite())
						.withAzimuth(Math.PI * 0.90)
						.withZenith(Math.PI * -0.20).getViewMatrix();
				//System.out.println(lightPos.toString());*/
				break;
			case KeyEvent.VK_M:
				saved = false;
				break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void dispose(GLAutoDrawable glDrawable) {
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();
		gl.glDeleteProgram(shaderProgram);
	}

	private void drawStrings() {
		textRenderer.drawStr2D(3, height - 15, "PGRF3 - task 2 | Controls: [LMB] camera, " +
				"[WASD] camera movement, [B] fill mode, [OP] rotateX, [E] anti-aliasing mode, [RMB] rotateZ");
		textRenderer.drawStr2D(width - 90, 3, " (c) Pavel Borik");
		if(aaSwitch == 0) textRenderer.drawStr2D(width - 800, 3, "AA mode: No anti-aliasing");
		if(aaSwitch == 1) textRenderer.drawStr2D(width - 800, 3, "AA mode: Poisson Sampling");
		if(aaSwitch == 2) textRenderer.drawStr2D(width - 800, 3, "AA mode: Stratified Poisson Sampling");


	}
}