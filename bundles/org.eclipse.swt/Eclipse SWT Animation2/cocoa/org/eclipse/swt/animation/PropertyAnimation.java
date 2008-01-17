package org.eclipse.swt.animation;

import java.lang.reflect.*;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.cocoa.*;
import org.eclipse.swt.widgets.*;

public class PropertyAnimation extends Animation {
	Object from, to;
	String property;
	Object target;
	Method method;
	SWTAnimationViewBase animationView;

	//FIXME??? don't know how to do custom interpolation with Cocoa
	IInterpolator interpolator;
 
	CABasicAnimation[] animations;
	boolean started;
	int completed;
	
	Class paramType;
	
	void animationDidStart(int id) {
		started = true;
	}
	
	void animationDidStop(int id, int finished) {
		if (finished > 0) completed++;
		if (parent != null && isFinished()) parent.childFinished(this); 
	}
	
	void animationUpdated(float progress) {
		System.out.println("val = " + progress);
		try {
			Object methodArg = null;
			if (paramType == Color.class) {
				Color color = getColor(progress);
				methodArg = color;
			}
			if (paramType == Double.class) {		
				double f = ((Double) from).doubleValue();
				double t = ((Double) to).doubleValue();
				double newValue = f + ((t - f) * progress);
				methodArg = new Double(newValue);
			}
			if (paramType == Integer.TYPE) {
				int f = ((Integer) from).intValue();
				int t = ((Integer) to).intValue();
				int newValue = (int) (f + ((t - f) * progress));
				methodArg = new Integer(newValue);
			}
			if (methodArg != null) {
				method.invoke(target, new Object[] {methodArg});
			} else {
				throw new RuntimeException(paramType + " not supported yet");
			}
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
	
	void create() {
		super.create();
		if (target == null || property == null) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		if (animations != null) {
			for (int i = 0; i < animations.length; i++) {
				if (animations[i] != null) animations[i].release();
			}
			animations = null;
		}
		createAnimations();
		for (int i = 0; i < animations.length; i++) {
			animations[i].setDelegate(delegate);
		}
		setTimingFunction();
		updateFromValue();
	}

	void createAnimations() {
		NSMutableDictionary dict = (NSMutableDictionary) NSMutableDictionary.dictionaryWithCapacity(2);
		if ("bounds".equalsIgnoreCase(property)) {
			animations = new CABasicAnimation[2];
			NSString keyPath = createNSString("frameOrigin");
			animations[0] = new CABasicAnimation(CABasicAnimation.animationWithKeyPath(keyPath).id);
			animations[0].retain();
			dict.setValue(animations[0], keyPath);
			keyPath = createNSString("frameSize");
			animations[1] = new CABasicAnimation(CABasicAnimation.animationWithKeyPath(keyPath).id);
			animations[1].retain();
			dict.setValue(animations[1], keyPath);
		}
		if ("alpha".equalsIgnoreCase(property)) {
			animations = new CABasicAnimation[1];
			NSString keyPath = createNSString("alphaValue");
			animations[0] = new CABasicAnimation(CABasicAnimation.animationWithKeyPath(keyPath).id);
			animations[0].retain();
			dict.setValue(animations[0], keyPath);
		}
		if (animations == null) {
			animations = new CABasicAnimation[1];
			NSString keyPath = createNSString("animationValue");
			animations[0] = new CABasicAnimation(CABasicAnimation.animationWithKeyPath(keyPath).id);
			animations[0].retain();
			dict.setValue(animations[0], keyPath);
			
			animationView = (SWTAnimationView) new SWTAnimationView().alloc().init();
			animationView.setTag(jniRef);
			((Control)target).view.addSubview_(animationView);
			
			//use reflection
			String mName = "set" + property.substring(0, 1).toUpperCase() + property.substring(1);
			Class clazz = target.getClass();
			Method[] methods = clazz.getMethods();
			for (int i = 0; i < methods.length; i++) {
				Method m = methods[i];
				if (m.getName().equals(mName)) {
					Class[] parameterTypes = m.getParameterTypes();
					if (parameterTypes.length == 1) {
						method = m;
						break;
					}
				}
			}
			paramType = method.getParameterTypes()[0];
		}
		int id = targetHandle();
		if (id != 0) OS.objc_msgSend(id, OS.sel_setAnimations_1, dict.id);
	}

	void createCustomAnimation() {
		/*
		* On WPF this is where we use a subclass of DoubleAnimation and override
		* GetNextValueCore to provide custom interpolation.
		*/
	}
	
	Color getColor(double newValue) {
		if (!(from instanceof Color && to instanceof Color)) return null;
		Color colorFrom = (Color) from;
		Color colorTo = (Color) to;
		RGB start = colorFrom.getRGB();
		RGB end = colorTo.getRGB();
		int red = (int) (((end.red-start.red)*newValue) + start.red);
		int green = (int) (((end.green-start.green)*newValue) + start.green);
		int blue = (int) (((end.blue-start.blue)*newValue) + start.blue);
		RGB current = new RGB(red, green, blue);
		return new Color(colorFrom.getDevice(), current);
	}
	
	public long getDuration() {
		return duration;
	}
	
	//TODO Control #getTransform, #setTransform()
	Transform getTransform(double newValue) {
		if (!(from instanceof Transform && to instanceof Transform)) return null;
//		Control control = (Control)target;
//		Transform transform = control.getTransform();
//		float[] f = new float[6];
//		for (int i = 0; i < 6; i++) {
//			f[i] = (float) (((transformTo[i]-transformFrom[i])*newValue) + transformFrom[i]);
//		}
//		transform.setElements(f[0], f[1], f[2], f[3], f[4], f[5]);
//		control.setTransform(transform); 
//		return transform;
		return null;
	}
	
	boolean isFinished() {
		return completed == animations.length;
	}
	
	boolean isRunning() {
		return started && !isFinished();
	}
	
	void release() {
		super.release();
		if (animations != null) {
			for (int i = 0; i < animations.length; i++) {
				if (animations[i] != null) animations[i].release();
			}
		}
		animations = null;
	}
		
	public void setDuration(long duration) {
		checkAnimation();
		this.duration = duration;
	}
	
	public void setFrom(Object from) {
		checkAnimation();
		this.from = from;
	}
	
	public void setInterpolator(IInterpolator interpolator) {
		checkAnimation();
		this.interpolator = interpolator;
	}
	
	public void setProperty(String property) {
		checkAnimation();
		this.property = property;
	}
	
	public void setTarget(Object target) {
		checkAnimation();
		this.target = target;
	}

	NSString createNSString(String path) {
		char[] buffer = new char[path.length()];
		path.getChars(0, buffer.length, buffer, 0);
		NSString keyPath = NSString.stringWithCharacters(buffer, buffer.length);
		return keyPath;
	}

	void setTimingFunction() {
	}
	
	public void setTo(Object to) {
		checkAnimation();
		this.to = to;
	}

	public void start(Widget widget) {
		super.start(widget);
		completed = 0;
		started = false;
		int id = targetHandle();
//		NSAnimationContext.beginGrouping();
		NSAnimationContext context = NSAnimationContext.currentContext();
		context.setDuration(duration/1000f);
		int animator = OS.objc_msgSend(id, OS.sel_animator);
		if ("bounds".equalsIgnoreCase(property)) {
			Rectangle rect = (Rectangle) to;
			NSRect nsRect = new NSRect();
			nsRect.height = rect.height;
			nsRect.width = rect.width;
			nsRect.x = rect.x;
			nsRect.y = rect.y;
			OS.objc_msgSend(animator, OS.sel_setFrame_1, nsRect);
		} 
		if ("alpha".equalsIgnoreCase(property)) {
			int alpha = ((Integer)to).intValue();
			float viewAlpha = (alpha & 0xFF) / (float) 0xFF;
			OS.objc_msgSend(animator, OS.sel_setAlphaValue_1, viewAlpha);
		}
		if (animationView != null) {
			OS.objc_msgSend(animator, OS.sel_setAnimationValue_1, 1.0f);
		}
//		NSAnimationContext.endGrouping();
	}
	
	public void stop() {
		int id = targetHandle();
		NSAnimationContext context = NSAnimationContext.currentContext();
		context.setDuration(0.0001f);
		int animator = OS.objc_msgSend(id, OS.sel_animator);
		if ("bounds".equalsIgnoreCase(property)) {
			NSRect nsRect = new NSRect();
			OS.objc_msgSend_stret(nsRect, id, OS.sel_frame);
			OS.objc_msgSend(animator, OS.sel_setFrame_1, nsRect);
		} 
		if ("alpha".equalsIgnoreCase(property)) {
			int viewAlpha = OS.objc_msgSend(id, OS.sel_alphaValue);
			OS.objc_msgSend(animator, OS.sel_setAlphaValue_1, viewAlpha);
		}
	}
	
	int targetHandle() {
		/*
		 * probably need some framework here to get us the right handle...
		 * int Widget.animationHandle() ???? 
		 */
		int result = 0;
		if (target instanceof Control) {
			Control control = (Control) target;
			result = control.view.id;
		} 
		if (animationView != null) {
			result = animationView.id;
		}

		/*
		 * FIXME
		 * if only the button has a layer, the button gets stretched
		 * when it is resized. This happens in simple cocoa app too.
		 * 
		 * if I set the layer on the superview, everything goes away
		 * in my java app (ie no controls visible), but this pattern 
		 * works well in my simple cocoa application.
		 * 
		 * if no control has a layer, we can't change the controls
		 * alpha value, and we don't get some of the performance
		 * benefits of CAAnimation.
		 */
		((Control) target).view.setWantsLayer(true);
//		((Control) target).view.superview().setWantsLayer(true);

		return result;
	}
	
	void updateFromValue() {
		if (property.equalsIgnoreCase("bounds")) {
			if (!(from instanceof Rectangle && to instanceof Rectangle)) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
			Rectangle rect = (Rectangle) from;
			NSPoint pt = new NSPoint();
			pt.x = rect.x;
			pt.y = rect.y;
			NSValue val = NSValue.valueWithPoint(pt);
			animations[0].setFromValue(val);
			NSSize size = new NSSize();
			size.height = rect.height;
			size.width = rect.width;
			val = NSValue.valueWithSize(size);
			animations[1].setFromValue(val);
		}
		if (property.equalsIgnoreCase("alpha")) {
			if (!(from instanceof Integer && to instanceof Integer)) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
			int alpha = ((Integer)from).intValue();
			float viewAlpha = (alpha & 0xFF) / (float) 0xFF;
			NSValue val = NSNumber.numberWithFloat(viewAlpha);
			animations[0].setFromValue(val);
		}
		if (animationView != null) {
			NSValue val = NSNumber.numberWithInt(0);
			animations[0].setFromValue(val);
		}
	}
}
