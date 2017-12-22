package oglutils;

import static oglutils.ShaderUtils.GEOMETRY_SHADER_SUPPORT_VERSION;
import static oglutils.ShaderUtils.TESSELATION_SUPPORT_VERSION;
import static oglutils.ShaderUtils.COMPUTE_SHADER_SUPPORT_VERSION;

import com.jogamp.opengl.DebugGL2;
import com.jogamp.opengl.DebugGL3;
import com.jogamp.opengl.DebugGL4;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLProfile;

public class OGLUtils {

	/**
	 * Print version, vendor and extensions of current OpenGL
	 * 
	 * @param gl
	 *            OpenGL context
	 */
	public static void printOGLparameters(GL2GL3 gl) {
		if (gl == null)
			return;

		System.out.println("GLProfile name: " + GLProfile.getDefault().getName());
		System.out.println("GLProfile implementation: " + GLProfile.getDefault().getName());
		System.out.println("GL class: " + gl.getClass().getName());
		System.out.println("GL vendor: " + gl.glGetString(GL2GL3.GL_VENDOR));
		System.out.println("GL renderer: " + gl.glGetString(GL2GL3.GL_RENDERER));
		System.out.println("GL version: " + gl.glGetString(GL2GL3.GL_VERSION));
		System.out.println("GL shading language version: " + gl.glGetString(GL2GL3.GL_SHADING_LANGUAGE_VERSION)
				+ " (#version " + getVersionGLSL(gl) + ")");
		System.out.println("GL extensions: " + getExtensions(gl));

	}

	/**
	 * Get extensions of current OpenGL
	 * 
	 */
	public static String getExtensions(GL2GL3 gl) {
		String extensions;
		if (getVersionGLSL(gl) < getVersionOpenGL(gl)) {
			// Deprecated in newer versions
			extensions = gl.glGetString(GL2GL3.GL_EXTENSIONS);
		} else {
			int[] numberExtensions = new int[1];
			gl.glGetIntegerv(GL2GL3.GL_NUM_EXTENSIONS, numberExtensions, 0);
			extensions = gl.glGetStringi(GL2GL3.GL_EXTENSIONS, 1);
			for (int i = 1; i < numberExtensions[0]; i++) {
				extensions = extensions + " " + gl.glGetStringi(GL2GL3.GL_EXTENSIONS, i);
			}
		}
		return extensions;
	}

	/**
	 * Get supported GLSL version
	 * 
	 * @return version as integer number multiplied by 100, for GLSL 1.4 return
	 *         140, for GLSL 4.5 return 450, ...
	 */
	public static int getVersionGLSL(GL2GL3 gl) {
		String version = new String(gl.glGetString(GL2GL3.GL_SHADING_LANGUAGE_VERSION));
		String[] parts = version.split(" ");
		parts = parts[0].split("\\.");
		int versionNumber = Integer.parseInt(parts[0]) * 100 + Integer.parseInt(parts[1]);
		return versionNumber;
	}

	/**
	 * Get supported OpenGL version
	 * 
	 * @return version as integer number multiplied by 100, for OpenGL 3.3
	 *         return 330, ...
	 */
	public static int getVersionOpenGL(GL2GL3 gl) {
		String version = new String(gl.glGetString(GL.GL_VERSION));
		String[] parts = version.split(" ");
		parts = parts[0].split("\\.");
		int versionNumber = Integer.parseInt(parts[0]) * 100 + Integer.parseInt(parts[1]) * 10;
		return versionNumber;
	}

	/**
	 * Print parameters of current JOGL
	 * 
	 */
	public static void printJOGLparameters() {
		Package p = Package.getPackage("com.jogamp.opengl");
		System.out.println("JOGL specification version: " + p.getSpecificationVersion());
		System.out.println("JOGL implementation version: " + p.getImplementationVersion());
		System.out.println("JOGL implementation title: " + p.getImplementationTitle());
		System.out.println("JOGL implementation vendor: " + p.getImplementationVendor());
	}

	/**
	 * Print parameters of current JAVA
	 * 
	 */
	public static void printJAVAparameters() {
		System.out.println("Java version: " + System.getProperty("java.version"));
		System.out.println("Java vendor: " + System.getProperty("java.vendor"));
	}

	/**
	 * Check OpenGL shaders support
	 * 
	 * @param gl
	 */
	public static void shaderCheck(GL2GL3 gl) {
		String extensions = gl.glGetString(GL.GL_EXTENSIONS);

		if ((OGLUtils.getVersionGLSL(gl) < getVersionOpenGL(gl)) && (extensions.indexOf("GL_ARB_vertex_shader") == -1
				|| extensions.indexOf("GL_ARB_fragment_shader") == -1)) {
			throw new RuntimeException("Shaders are not available.");
		}

		System.out.println("This OpenGL (#version " + getVersionGLSL(gl) + ") supports:\n vertex and fragment shader");

		if ((OGLUtils.getVersionGLSL(gl) >= GEOMETRY_SHADER_SUPPORT_VERSION)
				|| (extensions.indexOf("geometry_shader") != -1))
			System.out.println(" geometry shader");

		if ((OGLUtils.getVersionGLSL(gl) >= TESSELATION_SUPPORT_VERSION)
				|| (extensions.indexOf("tessellation_shader") != -1))
			System.out.println(" tessellation");

		if ((OGLUtils.getVersionGLSL(gl) >= COMPUTE_SHADER_SUPPORT_VERSION)
				|| (extensions.indexOf("compute_shader") != -1))
			System.out.println(" compute shader");
	}
	
	/**
	 * Return correct debug object
	 * 
	 * @param gl
	 * @return 
	 */
	public static GL2GL3 getDebugGL(GL2GL3 gl){
		int version = getVersionOpenGL(gl);
		if (version < 300)
			return new DebugGL2(gl.getGL2());
		if (version < 400)
			return new DebugGL3(gl.getGL3());
		return new DebugGL4(gl.getGL4());
	}

	/**
	 * Check GL error
	 * 
	 * @param gl
	 * @param longReport
	 *            type of report
	 */
	static public void checkGLError(GL2GL3 gl, String text, boolean longReport) {
		int err = gl.glGetError();
		String errorName, errorDesc;

		while (err != GL2GL3.GL_NO_ERROR) {

			switch (err) {
			case GL2GL3.GL_INVALID_ENUM:
				errorName = "GL_INVALID_ENUM";
				errorDesc = "An unacceptable value is specified for an enumerated argument. The offending command is ignored and has no other side effect than to set the error flag.";
				break;

			case GL2GL3.GL_INVALID_VALUE:
				errorName = "GL_INVALID_VALUE";
				errorDesc = "A numeric argument is out of range. The offending command is ignored and has no other side effect than to set the error flag.";
				break;

			case GL2GL3.GL_INVALID_OPERATION:
				errorName = "GL_INVALID_OPERATION";
				errorDesc = "The specified operation is not allowed in the current state. The offending command is ignored and has no other side effect than to set the error flag.";
				break;
			case GL2GL3.GL_INVALID_FRAMEBUFFER_OPERATION:
				errorName = "GL_INVALID_FRAMEBUFFER_OPERATION";
				errorDesc = "The framebuffer object is not complete. The offending command is ignored and has no other side effect than to set the error flag.";
				break;
			case GL2GL3.GL_OUT_OF_MEMORY:
				errorName = "GL_OUT_OF_MEMORY";
				errorDesc = "There is not enough memory left to execute the command. The state of the GL is undefined, except for the state of the error flags, after this error is recorded.";
				break;
			default:
				return;
			}
			if (longReport)
				System.err.println(text + " GL error: " + err + " " + errorName + ": " + errorDesc);
			else
				System.err.println(text + " GL error: " + errorName);
			err = gl.glGetError();
		}

	}

	/**
	 * Empty GL error
	 * 
	 * @param gl
	 */
	static public void emptyGLError(GL2GL3 gl) {
		int err = gl.glGetError();
		while (err != GL2GL3.GL_NO_ERROR) {
			err = gl.glGetError();
		}

	}

	/**
	 * Check GL error
	 * 
	 * @param gl
	 */
	static public void checkGLError(GL2GL3 gl, String text) {
		checkGLError(gl, text, false);
	}

	/**
	 * Check GL error
	 * 
	 * @param gl
	 */
	static public void checkGLError(GL2GL3 gl) {
		checkGLError(gl, "", false);
	}

}
