/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.opengl;

import org.eclipse.swt.*;
import org.eclipse.swt.opengl.internal.win32.*;
import org.eclipse.swt.internal.win32.*;
import org.eclipse.swt.widgets.*;

/**
 * GLCanvas is a widget capable of displaying OpenGL content.
 */

public class GLCanvas extends Canvas {
	int context;

/**
 * Create a GLCanvas widget using the attributes described in the GLData
 * object provided.
 *
 * @param parent a composite widget
 * @param style the bitwise OR'ing of widget styles
 * @param data the requested attributes of the GLCanvas
 *
 * @exception IllegalArgumentException
 * <ul><li>ERROR_NULL_ARGUMENT when the data is null
 *     <li>ERROR_UNSUPPORTED_DEPTH when the requested attributes cannot be provided</ul> 
 * @exception SWTException
 * <ul><li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread
 *     <li>ERROR_CANNOT_CREATE_OBJECT when failed to create OLE Object
 *     <li>ERROR_CANNOT_OPEN_FILE when failed to open file
 *     <li>ERROR_INTERFACE_NOT_FOUND when unable to create callbacks for OLE Interfaces
 *     <li>ERROR_INVALID_CLASSID
 * </ul>
 */
public GLCanvas (Composite parent, int style, GLData data) {
	super (parent, style);
	if (data == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);

	PIXELFORMATDESCRIPTOR pfd = new PIXELFORMATDESCRIPTOR ();
	pfd.nSize = (short) PIXELFORMATDESCRIPTOR.sizeof;
	pfd.nVersion = 1;
	pfd.dwFlags = WGL.PFD_DRAW_TO_WINDOW | WGL.PFD_SUPPORT_OPENGL;
	pfd.dwLayerMask = WGL.PFD_MAIN_PLANE;
	pfd.iPixelType = (byte) WGL.PFD_TYPE_RGBA;
	if (data.doubleBuffer) pfd.dwFlags |= WGL.PFD_DOUBLEBUFFER;
	if (data.stereo) pfd.dwFlags |= WGL.PFD_STEREO;
	pfd.cRedBits = (byte) data.redSize;
	pfd.cGreenBits = (byte) data.greenSize;
	pfd.cBlueBits = (byte) data.blueSize;
	pfd.cAlphaBits = (byte) data.alphaSize;
	pfd.cDepthBits = (byte) data.depthSize;
	pfd.cStencilBits = (byte) data.stencilSize;
	pfd.cAccumRedBits = (byte) data.accumRedSize;
	pfd.cAccumGreenBits = (byte) data.accumGreenSize;
	pfd.cAccumBlueBits = (byte) data.accumBlueSize;
	pfd.cAccumAlphaBits = (byte) data.accumAlphaSize;
	pfd.cAccumBits = (byte) (pfd.cAccumRedBits + pfd.cAccumGreenBits + pfd.cAccumBlueBits + pfd.cAccumAlphaBits);
	//FIXME - use wglChoosePixelFormatARB
//	if (data.sampleBuffers > 0) {
//		wglAttrib [pos++] = WGL.WGL_SAMPLE_BUFFERS_ARB;
//		wglAttrib [pos++] = data.sampleBuffers;
//	}
//	if (data.samples > 0) {
//		wglAttrib [pos++] = WGL.WGL_SAMPLES_ARB;
//		wglAttrib [pos++] = data.samples;
//	}

	int hDC = OS.GetDC (handle);
	int pixelFormat = WGL.ChoosePixelFormat (hDC, pfd);
	if (pixelFormat == 0) {
		OS.ReleaseDC (handle, hDC);
		SWT.error (SWT.ERROR_UNSUPPORTED_DEPTH);
	}
	if (!WGL.SetPixelFormat (hDC, pixelFormat, pfd)) {
		OS.ReleaseDC (handle, hDC);
		SWT.error (SWT.ERROR_UNSUPPORTED_DEPTH);
	}
	context = WGL.wglCreateContext (hDC);
	if (context == 0) {
		OS.ReleaseDC (handle, hDC);
		SWT.error (SWT.ERROR_NO_HANDLES);
	}
	OS.ReleaseDC (handle, hDC);
//	if (share != null) {
//		WGL.wglShareLists (context, share.context);
//	}
	
	Listener listener = new Listener () {
		public void handleEvent (Event event) {
			switch (event.type) {
			case SWT.Dispose:
				WGL.wglDeleteContext (context);
				break;
			}
		}
	};
	addListener (SWT.Dispose, listener);
}

/**
 * Returns a boolean indicating whether the receiver's OpenGL context
 * is the current context.
 *  
 * @return true if the receiver holds the current OpenGL context,
 * false otherwise
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public boolean isCurrent () {
	checkWidget ();
	return WGL.wglGetCurrentContext () == handle;
}

/**
 * Sets the OpenGL context associated with this GLCanvas to be the
 * current GL context.
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setCurrent () {
	checkWidget ();
	if (WGL.wglGetCurrentContext () == handle) return;
	int hDC = OS.GetDC (handle);
	WGL.wglMakeCurrent (hDC, context);
	OS.ReleaseDC (handle, hDC);
}

/**
 * Swaps the front and back color buffers.
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void swapBuffers () {
	checkWidget ();
	int hDC = OS.GetDC (handle);
	WGL.SwapBuffers (hDC);
	OS.ReleaseDC (handle, hDC);
}
}
