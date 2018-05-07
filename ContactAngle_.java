/*====================================================================
| Contact angle calculation on digital images with different methods
| Last Version: sept 20, 2005
\===================================================================*/

/*====================================================================
| di Marco Brugnara
| Dipartimento di Ingegneria dei Materiali e Tecnologie Industriali
| Laboratorio Polimeri e Compositi
| via Mesiano 77 38050 Povo (TN)
| tel ufficio 0461 882483
| email: marco.brugnara@ing.unitn.it
| web page: http://www.ing.unitn.it/~brugnara/
| Based on the Pointpicker plugin 
| http://bigwww.epfl.ch/thevenaz/pointpicker/
\===================================================================*/

import ij.gui.GUI;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.WindowManager;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Event;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import Jama.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;



import java.awt.*;
import java.awt.image.*;
import java.util.Vector;
import java.io.*;
import ij.*;
import ij.io.*;


/*====================================================================
|	ContactAngle
\===================================================================*/

/*********************************************************************
 * This class is the only one that is accessed directly by imageJ;
 * it attaches listeners and dies. Note that it implements
 * <code>PlugIn</code> rather than <code>PlugInFilter</code>.
 ********************************************************************/
public class ContactAngle_
	implements
		PlugIn

{ /* begin class ContactAngle */

/*..................................................................*/
/* Public methods						    */
/*..................................................................*/

/*------------------------------------------------------------------*/
public void run (
	final String arg
) {
	final ImagePlus imp = WindowManager.getCurrentImage();
	if (imp == null) {
		IJ.noImage();
		return;
	}
	final ImageCanvas ic = imp.getWindow().getCanvas();
	final caToolbar tb = new caToolbar(Toolbar.getInstance());
	final caHandler ph = new caHandler(imp, tb);
	tb.setWindow(ph, imp);	
	//IJ.run("8-bit","slice");
} /* end run */
} /* end class ContactAngle*/

/*====================================================================
|	caAct
\===================================================================*/

/*********************************************************************
 * This class is responsible for dealing with the mouse events relative
 * to the image window.
 ********************************************************************/

 class caAct
	extends
		ImageCanvas
	implements
		KeyListener,
		MouseListener,
		MouseMotionListener

{ /* begin class caAct */

/*....................................................................
	Public variables
....................................................................*/
public static final int ADD_CROSS = 0;
public static final int MOVE_CROSS = 1;
public static final int REMOVE_CROSS = 2;
public static final int FILE = 3;
public static final int TERMINATE = 4;
public static final int BOTH_BEST = 6;
public static final int MAGNIFIER = 11;

/*....................................................................
	Private variables
....................................................................*/
private ImagePlus imp;
private caHandler ph;
private caToolbar tb;
private boolean frontmost = false;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 * Return true if the window is frontmost.
 ********************************************************************/
public boolean isFrontmost (
) {
	return(frontmost);
} /* end isFrontmost */

/*********************************************************************
 * Listen to <code>keyPressed</code> events.
 *
 * @param e The expected key codes are as follows:
 * <ul><li><code>KeyEvent.VK_DELETE</code>: remove the current landmark;</li>
 * <li><code>KeyEvent.VK_BACK_SPACE</code>: remove the current landmark;</li>
 * <li><code>KeyEvent.VK_DOWN</code>: move down the current landmark;</li>
 * <li><code>KeyEvent.VK_LEFT</code>: move the current landmark to the left;</li>
 * <li><code>KeyEvent.VK_RIGHT</code>: move the current landmark to the right;</li>
 * <li><code>KeyEvent.VK_TAB</code>: activate the next landmark;</li>
 * <li><code>KeyEvent.VK_UP</code>: move up the current landmark.</li></ul>
 * <li><code>KeyEvent.VK_SEMICOLON</code>: run bothbest and exit.</li></ul>
 ********************************************************************/
public void keyPressed (
	final KeyEvent e
) {
	frontmost = true;
	final Point p = ph.getPoint();
	if (p == null) {
		frontmost = false;
		return;
	}
	final int x = p.x;
	final int y = p.y;
	switch (e.getKeyCode()) {
		case KeyEvent.VK_DELETE:
		case KeyEvent.VK_BACK_SPACE:
			ph.removePoint();
			break;
		case KeyEvent.VK_DOWN:
			ph.movePoint(imp.getWindow().getCanvas().screenX(x),
				imp.getWindow().getCanvas().screenY(y
				+ (int)Math.ceil(1.0 / imp.getWindow().getCanvas().getMagnification())));
			break;
		case KeyEvent.VK_LEFT:
			ph.movePoint(imp.getWindow().getCanvas().screenX(x
				- (int)Math.ceil(1.0 / imp.getWindow().getCanvas().getMagnification())),
				imp.getWindow().getCanvas().screenY(y));
			break;
		case KeyEvent.VK_RIGHT:
			ph.movePoint(imp.getWindow().getCanvas().screenX(x
				+ (int)Math.ceil(1.0 / imp.getWindow().getCanvas().getMagnification())),
				imp.getWindow().getCanvas().screenY(y));
			break;
		case KeyEvent.VK_TAB:
			ph.nextPoint();
			break;
		case KeyEvent.VK_UP:
			ph.movePoint(imp.getWindow().getCanvas().screenX(x),
				imp.getWindow().getCanvas().screenY(y
				- (int)Math.ceil(1.0 / imp.getWindow().getCanvas().getMagnification())));
			break;
		case KeyEvent.VK_SEMICOLON:
			new caFile(IJ.getInstance(), ph, imp).actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Both Bestfits"));
			ph.cleanUp();
			tb.restorePreviousToolbar();
			Toolbar.getInstance().repaint();
			break;
	}
	imp.setRoi(ph);
} /* end keyPressed */

/*********************************************************************
 * Listen to <code>keyReleased</code> events.
 *
 * @param e Ignored.
 ********************************************************************/
public void keyReleased (
	final KeyEvent e
) {
	frontmost = true;
} /* end keyReleased */

/*********************************************************************
 * Listen to <code>keyTyped</code> events.
 *
 * @param e Ignored.
 ********************************************************************/
public void keyTyped (
	final KeyEvent e
) {
	frontmost = true;
} /* end keyTyped */

/*********************************************************************
 * Listen to <code>mouseClicked</code> events.
 *
 * @param e Ignored.
 ********************************************************************/
public void mouseClicked (
	final MouseEvent e
) {
	frontmost = true;
} /* end mouseClicked */

/*********************************************************************
 * Listen to <code>mouseDragged</code> events. Move the current point
 * and refresh the image window.
 *
 * @param e Event.
 ********************************************************************/
public void mouseDragged (
	final MouseEvent e
) {
	frontmost = true;
	final int x = e.getX();
	final int y = e.getY();
	if (tb.getCurrentTool() == MOVE_CROSS) {
		ph.movePoint(x, y);
		imp.setRoi(ph);
	}
	mouseMoved(e);
} /* end mouseDragged */

/*********************************************************************
 * Listen to <code>mouseEntered</code> events.
 *
 * @param e Ignored.
 ********************************************************************/
public void mouseEntered (
	final MouseEvent e
) {
//	frontmost = true;
//	WindowManager.setCurrentWindow(imp.getWindow());
//	imp.getWindow().toFront();
//	imp.setRoi(ph);
} /* end mouseEntered */

/*********************************************************************
 * Listen to <code>mouseExited</code> events. Clear the ImageJ status
 * bar.
 *
 * @param e Event.
 ********************************************************************/
public void mouseExited (
	final MouseEvent e
) {
//	frontmost = false;
//	imp.getWindow().toBack();
//	IJ.getInstance().toFront();
//	imp.setRoi(ph);
//	IJ.showStatus("");
} /* end mouseExited */

/*********************************************************************
 * Listen to <code>mouseMoved</code> events. Update the ImageJ status
 * bar.
 *
 * @param e Event.
 ********************************************************************/
public void mouseMoved (
	final MouseEvent e
) {
	frontmost = true;
	setControl();
	final int x = imp.getWindow().getCanvas().offScreenX(e.getX());
	final int y = imp.getWindow().getCanvas().offScreenY(e.getY());
	IJ.showStatus(imp.getLocationAsString(x, y) + getValueAsString(x, y));
} /* end mouseMoved */

/*********************************************************************
 * Listen to <code>mousePressed</code> events. Perform the relevant
 * action.
 *
 * @param e Event.
 ********************************************************************/
public void mousePressed (
	final MouseEvent e
) {
	frontmost = true;
	final int x = e.getX();
	final int y = e.getY();
	int currentPoint;
	switch (tb.getCurrentTool()) {
		case ADD_CROSS:
			ph.addPoint(imp.getWindow().getCanvas().offScreenX(x),
				imp.getWindow().getCanvas().offScreenY(y));
			break;
		case MAGNIFIER:
			final int flags = e.getModifiers();
			if ((flags & (Event.ALT_MASK | Event.META_MASK | Event.CTRL_MASK)) != 0) {
				imp.getWindow().getCanvas().zoomOut(x, y);
			}
			else {
				imp.getWindow().getCanvas().zoomIn(x, y);
			}
			break;
		case MOVE_CROSS:
			ph.findClosest(x, y);
			break;
		case REMOVE_CROSS:
			ph.findClosest(x, y);
			ph.removePoint();
			break;
	}
	imp.setRoi(ph);
} /* end mousePressed */

/*********************************************************************
 * Listen to <code>mouseReleased</code> events.
 *
 * @param e Ignored.
 ********************************************************************/
public void mouseReleased (
	final MouseEvent e
) {
	frontmost = true;
} /* end mouseReleased */

/*********************************************************************
 * This constructor stores a local copy of its parameters and initializes
 * the current control.
 *
 * @param imp <code>ImagePlus<code> object where points are being picked.
 * @param ph <code>caHandler<code> object that handles operations.
 * @param tb <code>caToolbar<code> object that handles the toolbar.
 ********************************************************************/
public caAct (
	final ImagePlus imp,
	final caHandler ph,
	final caToolbar tb
) {
	super(imp);
	this.imp = imp;
	this.ph = ph;
	this.tb = tb;
} /* end caAct */

/*....................................................................
	Private methods
....................................................................*/

/*------------------------------------------------------------------ */
private String getValueAsString (
	final int x,
	final int y
) {
	final Calibration cal = imp.getCalibration();
	final int[] v = imp.getPixel(x, y);
	switch (imp.getType()) {
		case ImagePlus.GRAY8:
		case ImagePlus.GRAY16:
			final double cValue = cal.getCValue(v[0]);
			if (cValue==v[0]) {
				return(", value=" + v[0]);
			}
			else {
				return(", value=" + IJ.d2s(cValue) + " (" + v[0] + ")");
			}
		case ImagePlus.GRAY32:
			return(", value=" + Float.intBitsToFloat(v[0]));
		case ImagePlus.COLOR_256:
			return(", index=" + v[3] + ", value=" + v[0] + "," + v[1] + "," + v[2]);
		case ImagePlus.COLOR_RGB:
			return(", value=" + v[0] + "," + v[1] + "," + v[2]);
		default:
			return("");
	}
	
}  /* end getValueAsString */

/*------------------------------------------------------------------*/
private void setControl (
) {
	switch (tb.getCurrentTool()) {
		case ADD_CROSS:
			imp.getWindow().getCanvas().setCursor(crosshairCursor);
			break;
		case FILE:
		case MAGNIFIER:
		case MOVE_CROSS:
		case REMOVE_CROSS:
			imp.getWindow().getCanvas().setCursor(defaultCursor);
		default:
			break;
	}
} /* end setControl */

} /* end class caAct */

/*====================================================================
|	caHandler
\===================================================================*/

/*********************************************************************
 * This class is responsible for dealing with the list of point
 * coordinates and for their visual appearance.
 ********************************************************************/
class caHandler
	extends
		Roi

{ /* begin class caHandler */

/*....................................................................
	Public variables
....................................................................*/
public static final int RAINBOW = 1;
public static final int MONOCHROME = 2;

/*....................................................................
	Private variables
....................................................................*/
private static final int GAMUT = 1024;
private static final int CROSS_HALFSIZE = 5;
private final Color spectrum[] = new Color[GAMUT];
private final boolean usedColor[] = new boolean[GAMUT];
private final Vector listColors = new Vector(0, 16);
private final Vector listPoints = new Vector(0, 16);
private ImagePlus imp;
private caAct pa;
private caToolbar tb;
private int currentColor = 0;
private int currentPoint = -1;
private int numPoints = 0;
private boolean started = false;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 * This method adds a new point to the list, with a color that is as
 * different as possible from all those that are already in use. The
 * points are stored in pixel units rather than canvas units to cope
 * for different zooming factors.
 *
 * @param x Horizontal coordinate, in canvas units.
 * @param y Vertical coordinate, in canvas units.
 ********************************************************************/
public void addPoint (
	final int x,
	final int y
) {
	if (numPoints < GAMUT) {
		final Point p = new Point(x, y);
		listPoints.addElement(p);
		if (!usedColor[currentColor]) {
			usedColor[currentColor] = true;
		}
		else {
			int k;
			for (k = 0; (k < GAMUT); k++) {
				currentColor++;
				currentColor &= GAMUT - 1;
				if (!usedColor[currentColor]) {
					break;
				}
			}
			if (GAMUT <= k) {
				throw new IllegalStateException("Unexpected lack of available colors");
			}
		}
		int stirredColor = 0;
		int c = currentColor;
		for (int k = 0; (k < (int)Math.round(Math.log((double)GAMUT) / Math.log(2.0))); k++) {
			stirredColor <<= 1;
			stirredColor |= (c & 1);
			c >>= 1;
		}
		listColors.addElement(new Integer(stirredColor));
		currentColor++;
		currentColor &= GAMUT - 1;
		currentPoint = numPoints;
		numPoints++;
	}
	else {
		IJ.error("Maximum number of points reached");
	}
} /* end addPoint */

/*********************************************************************
 * Restore the listeners
 ********************************************************************/
public void cleanUp (
) {
	removePoints();
	final ImageCanvas ic = imp.getWindow().getCanvas();
	ic.removeKeyListener(pa);
	ic.removeMouseListener(pa);
	ic.removeMouseMotionListener(pa);
	ic.addMouseMotionListener(ic);
	ic.addMouseListener(ic);
	ic.addKeyListener(IJ.getInstance());
} /* end cleanUp */

/*********************************************************************
 * Draw the landmarks and outline the current point if there is one.
 *
 * @param g Graphics environment.
 ********************************************************************/
public void draw (
	final Graphics g
) {
	if (started) {
		final float mag = (float)ic.getMagnification();
		final int dx = (int)(mag / 2.0);
		final int dy = (int)(mag / 2.0);
		for (int k = 0; (k < listPoints.size()); k++) {
			final Point p = (Point)listPoints.elementAt(k);
			g.setColor(spectrum[((Integer)listColors.elementAt(k)).intValue()]);
			if (k == currentPoint) {
				if (pa.isFrontmost()) {
					g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y - 1) + dy,
						ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y - 1) + dy);
					g.drawLine(ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y - 1) + dy,
						ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy);
					g.drawLine(ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy,
						ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy);
					g.drawLine(ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy,
						ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y - 1) + dy);
					g.drawLine(ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y - 1) + dy,
						ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y - 1) + dy);
					g.drawLine(ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y - 1) + dy,
						ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y + 1) + dy);
					g.drawLine(ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y + 1) + dy,
						ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y + 1) + dy);
					g.drawLine(ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y + 1) + dy,
						ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy);
					g.drawLine(ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy,
						ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy);
					g.drawLine(ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy,
						ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y + 1) + dy);
					g.drawLine(ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y + 1) + dy,
						ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y + 1) + dy);
					g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y + 1) + dy,
						ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y - 1) + dy);
					if (1.0 < ic.getMagnification()) {
						g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE) + dx,
							ic.screenY(p.y) + dy,
							ic.screenX(p.x + CROSS_HALFSIZE) + dx,
							ic.screenY(p.y) + dy);
						g.drawLine(ic.screenX(p.x) + dx,
							ic.screenY(p.y - CROSS_HALFSIZE) + dy,
							ic.screenX(p.x) + dx,
							ic.screenY(p.y + CROSS_HALFSIZE) + dy);
					}
				}
				else {
					g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE + 1) + dy,
						ic.screenX(p.x + CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE - 1) + dy);
					g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE - 1) + dy,
						ic.screenX(p.x + CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE + 1) + dy);
				}
			}
			else {
				g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE) + dx,
					ic.screenY(p.y) + dy,
					ic.screenX(p.x + CROSS_HALFSIZE) + dx,
					ic.screenY(p.y) + dy);
				g.drawLine(ic.screenX(p.x) + dx,
					ic.screenY(p.y - CROSS_HALFSIZE) + dy,
					ic.screenX(p.x) + dx,
					ic.screenY(p.y + CROSS_HALFSIZE) + dy);
			}
		}
		if (updateFullWindow) {
			updateFullWindow = false;
			imp.draw();
		}
	}
} /* end draw */

/*********************************************************************
 * Let the point that is closest to the given coordinates become the
 * current landmark.
 *
 * @param x Horizontal coordinate, in canvas units.
 * @param y Vertical coordinate, in canvas units.
 ********************************************************************/
public void findClosest (
	int x,
	int y
) {
	if (listPoints.size() == 0) {
		currentPoint = -1;
		return;
	}
	x = ic.offScreenX(x);
	y = ic.offScreenY(y);
	Point p = new Point((Point)listPoints.elementAt(currentPoint));
	float distance = (float)(x - p.x) * (float)(x - p.x)
		+ (float)(y - p.y) * (float)(y - p.y);
	for (int k = 0; (k < listPoints.size()); k++) {
		p = (Point)listPoints.elementAt(k);
		final float candidate = (float)(x - p.x) * (float)(x - p.x)
			+ (float)(y - p.y) * (float)(y - p.y);
		if (candidate < distance) {
			distance = candidate;
			currentPoint = k;
		}
	}
} /* end findClosest */

/*********************************************************************
 * Return the current point as a <code>Point</code> object.
 ********************************************************************/
public Point getPoint (
) {
	return((0 <= currentPoint) ? (Point)listPoints.elementAt(currentPoint) : (null));
} /* end getPoint */

/*********************************************************************
 * Return the list of points.
 ********************************************************************/
public Vector getPoints (
) {
	return(listPoints);
} /* end getPoints */

/*********************************************************************
 * Modify the location of the current point. Clip the admissible range
 * to the image size.
 *
 * @param x Desired new horizontal coordinate in canvas units.
 * @param y Desired new vertical coordinate in canvas units.
 ********************************************************************/
public void movePoint (
	int x,
	int y
) {
	if (0 <= currentPoint) {
		x = ic.offScreenX(x);
		y = ic.offScreenY(y);
		x = (x < 0) ? (0) : (x);
		x = (imp.getWidth() <= x) ? (imp.getWidth() - 1) : (x);
		y = (y < 0) ? (0) : (y);
		y = (imp.getHeight() <= y) ? (imp.getHeight() - 1) : (y);
		listPoints.removeElementAt(currentPoint);
		final Point p = new Point(x, y);
		listPoints.insertElementAt(p, currentPoint);
	}
} /* end movePoint */

/*********************************************************************
 * Change the current point.
 ********************************************************************/
public void nextPoint (
) {
	currentPoint = (currentPoint == (numPoints - 1)) ? (0) : (currentPoint + 1);
} /* end nextPoint */

/*********************************************************************
 * This constructor stores a local copy of its parameters and initializes
 * the current spectrum. It also creates the object that takes care of
 * the interactive work.
 *
 * @param imp <code>ImagePlus<code> object where points are being picked.
 * @param tb <code>caToolbar<code> object that handles the toolbar.
 ********************************************************************/
public caHandler (
	final ImagePlus imp,
	final caToolbar tb
) {
	super(0, 0, imp.getWidth(), imp.getHeight(), imp);
	this.imp = imp;
	this.tb = tb;
	pa = new caAct(imp, this, tb);
	final ImageCanvas ic = imp.getWindow().getCanvas();
	ic.removeKeyListener(IJ.getInstance());
	ic.removeMouseListener(ic);
	ic.removeMouseMotionListener(ic);
	ic.addMouseMotionListener(pa);
	ic.addMouseListener(pa);
	ic.addKeyListener(pa);
	setSpectrum(RAINBOW);
	started = true;
} /* end caHandler */

/*********************************************************************
 * Remove the current point. Make its color available again.
 ********************************************************************/
public void removePoint (
) {
	if (0 < listPoints.size()) {
		listPoints.removeElementAt(currentPoint);
		usedColor[((Integer)listColors.elementAt(currentPoint)).intValue()] = false;
		listColors.removeElementAt(currentPoint);
		numPoints--;
	}
	currentPoint = numPoints - 1;
	if (currentPoint < 0) {
		tb.setTool(pa.ADD_CROSS);
	}
} /* end removePoint */

/*********************************************************************
 * Remove all points and make every color available.
 ********************************************************************/
public void removePoints (
) {
	listPoints.removeAllElements();
	listColors.removeAllElements();
	for (int k = 0; (k < GAMUT); k++) {
		usedColor[k] = false;
	}
	currentColor = 0;
	numPoints = 0;
	currentPoint = -1;
	tb.setTool(pa.ADD_CROSS);
	imp.setRoi(this);
} /* end removePoints */

/*********************************************************************
 * Setup the color scheme.
 *
 * @ param colorization Colorization code. Admissible values are
 * {<code>RAINBOW</code>, <code>MONOCHROME</code>}.
 ********************************************************************/
public void setSpectrum (
	final int colorization
) {
	int k = 0;
	switch (colorization) {
		case RAINBOW:
			final int bound1 = GAMUT / 6;
			final int bound2 = GAMUT / 3;
			final int bound3 = GAMUT / 2;
			final int bound4 = (2 * GAMUT) / 3;
			final int bound5 = (5 * GAMUT) / 6;
			final int bound6 = GAMUT;
			final float gamutChunk1 = (float)bound1;
			final float gamutChunk2 = (float)(bound2 - bound1);
			final float gamutChunk3 = (float)(bound3 - bound2);
			final float gamutChunk4 = (float)(bound4 - bound3);
			final float gamutChunk5 = (float)(bound5 - bound4);
			final float gamutChunk6 = (float)(bound6 - bound5);
			do {
				spectrum[k] = new Color(1.0F, (float)k / gamutChunk1, 0.0F);
				usedColor[k] = false;
			} while (++k < bound1);
			do {
				spectrum[k] = new Color(1.0F - (float)(k - bound1) / gamutChunk2, 1.0F, 0.0F);
				usedColor[k] = false;
			} while (++k < bound2);
			do {
				spectrum[k] = new Color(0.0F, 1.0F, (float)(k - bound2) / gamutChunk3);
				usedColor[k] = false;
			} while (++k < bound3);
			do {
				spectrum[k] = new Color(0.0F, 1.0F - (float)(k - bound3) / gamutChunk4, 1.0F);
				usedColor[k] = false;
			} while (++k < bound4);
			do {
				spectrum[k] = new Color((float)(k - bound4) / gamutChunk5, 0.0F, 1.0F);
				usedColor[k] = false;
			} while (++k < bound5);
			do {
				spectrum[k] = new Color(1.0F, 0.0F, 1.0F - (float)(k - bound5) / gamutChunk6);
				usedColor[k] = false;
			} while (++k < bound6);
			break;
		case MONOCHROME:
			for (k = 0; (k < GAMUT); k++) {
				spectrum[k] = ROIColor;
				usedColor[k] = false;
			}
			break;
	}
	imp.setRoi(this);
} /* end setSpectrum */

} /* end class caHandler */

/*====================================================================
|	caClearAll
\===================================================================*/

/*********************************************************************
 * This class creates a dialog to remove every point.
 ********************************************************************/
class caClearAll
	extends
		Dialog
	implements
		ActionListener

{ /* begin class caClearAll */

/*....................................................................
	Private variables
....................................................................*/
private caHandler ph;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 * This method processes the button actions.
 *
 * @param ae The expected actions are as follows:
 * <ul><li><code>Clear All</code>: Remove everything;</li>
 * <li><code>Cancel</code>: Do nothing.</li></ul>
 ********************************************************************/
public void actionPerformed (
	final ActionEvent ae
) {
	if (ae.getActionCommand().equals("Clear All")) {
		ph.removePoints();
		setVisible(false);
	}
	else if (ae.getActionCommand().equals("Cancel")) {
		setVisible(false);
	}
} /* end actionPerformed */

/*********************************************************************
 * Return some additional margin to the dialog, for aesthetic purposes.
 * Necessary for the current MacOS X Java version, lest the first item
 * disappears from the frame.
 ********************************************************************/
public Insets getInsets (
) {
	return(new Insets(0, 20, 20, 20));
} /* end getInsets */

/*********************************************************************
 * This constructor stores a local copy of its parameters and prepares
 * the layout of the dialog.
 *
 * @param parentWindow Parent window.
 * @param ph <code>caHandler<code> object that handles operations.
 ********************************************************************/
caClearAll (
	final Frame parentWindow,
	final caHandler ph
) {
	super(parentWindow, "Removing Points", true);
	this.ph = ph;
	setLayout(new GridLayout(0, 1));
	final Button removeButton = new Button("Clear All");
	removeButton.addActionListener(this);
	final Button cancelButton = new Button("Cancel");
	cancelButton.addActionListener(this);
	final Label separation1 = new Label("");
	final Label separation2 = new Label("");
	add(separation1);
	add(removeButton);
	add(separation2);
	add(cancelButton);
	pack();
} /* end caClearAll */

} /* end class caClearAll */

/*====================================================================
|	caColorSettings
\===================================================================*/

/*********************************************************************
 * This class creates a dialog to choose the color scheme.
 ********************************************************************/
class caColorSettings
	extends
		Dialog
	implements
		ActionListener

{ /* begin class caColorSettings */

/*....................................................................
	Private variables
....................................................................*/
private final CheckboxGroup choice = new CheckboxGroup();
private caHandler ph;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 * This method processes the button actions.
 *
 * @param ae The expected actions are as follows:
 * <ul><li><code>Rainbow</code>: Display points in many colors;</li>
 * <li><code>Monochrome</code>: Display points in ImageJ's highlight color;</li>
 * <li><code>Cancel</code>: Do nothing.</li></ul>
 ********************************************************************/
public void actionPerformed (
	final ActionEvent ae
) {
	if (ae.getActionCommand().equals("Rainbow")) {
		ph.setSpectrum(caHandler.RAINBOW);
		setVisible(false);
	}
	else if (ae.getActionCommand().equals("Monochrome")) {
		ph.setSpectrum(caHandler.MONOCHROME);
		setVisible(false);
	}
	else if (ae.getActionCommand().equals("Cancel")) {
		setVisible(false);
	}
} /* end actionPerformed */

/*********************************************************************
 * Return some additional margin to the dialog, for aesthetic purposes.
 * Necessary for the current MacOS X Java version, lest the first item
 * disappears from the frame.
 ********************************************************************/
public Insets getInsets (
) {
	return(new Insets(0, 20, 20, 20));
} /* end getInsets */

/*********************************************************************
 * This constructor stores a local copy of its parameters and prepares
 * the layout of the dialog.
 *
 * @param parentWindow Parent window.
 * @param ph <code>caHandler<code> object that handles operations.
 ********************************************************************/
caColorSettings (
	final Frame parentWindow,
	final caHandler ph
) {
	super(parentWindow, "Color Settings", true);
	this.ph = ph;
	setLayout(new GridLayout(0, 1));
	final Button rainbow = new Button("Rainbow");
	final Button monochrome = new Button("Monochrome");
	final Button cancelButton = new Button("Cancel");
	rainbow.addActionListener(this);
	monochrome.addActionListener(this);
	cancelButton.addActionListener(this);
	final Label separation1 = new Label("");
	final Label separation2 = new Label("");
	add(separation1);
	add(rainbow);
	add(monochrome);
	add(separation2);
	add(cancelButton);
	pack();
} /* end caColorSettings */

} /* end class caColorSettings */


/*********************************************************************
 * This class creates a dialog to set lower and upper Threshold
 ********************************************************************/
class caSetThreshold
	extends
		Dialog
	implements
		ActionListener

{ /* begin class caSetThreshold */

/*....................................................................
	Private variables
....................................................................*/
private TextField lowerT_Text;
private TextField upperT_Text;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 * This method processes the button actions.
 *
 * @param ae The expected actions are as follows:
 * <ul><li><code>Clear All</code>: Remove everything;</li>
 * <li><code>Cancel</code>: Do nothing.</li></ul>
 ********************************************************************/
	public void actionPerformed(final ActionEvent ae) {
		if (ae.getActionCommand().equals("Set Threshold")) {
			double lowerT = Double.valueOf(lowerT_Text.getText()).doubleValue();
			double upperT = Double.valueOf(upperT_Text.getText()).doubleValue();
			IJ.setThreshold(lowerT, upperT);
			//IJ.run("Threshold");
		} else if (ae.getActionCommand().equals("Reset Threshold")) {
			IJ.resetThreshold();
			//IJ.run("Threshold");
		} 
		else if (ae.getActionCommand().equals("OK")) {
			double lowerT = Double.valueOf(lowerT_Text.getText()).doubleValue();
			double upperT = Double.valueOf(upperT_Text.getText()).doubleValue();
			IJ.setThreshold(lowerT, upperT);
			//IJ.run("Threshold","slice");
			IJ.run("Convert to Mask", "slice");
			setVisible(false);
		}
		else if (ae.getActionCommand().equals("Cancel")) {
			setVisible(false);
		}
	} /* end actionPerformed */

/*********************************************************************
 * Return some additional margin to the dialog, for aesthetic purposes.
 * Necessary for the current MacOS X Java version, lest the first item
 * disappears from the frame.
 ********************************************************************/
/*public Insets getInsets (
) {
	return(new Insets(0, 20, 20, 20));
} */

/*********************************************************************
 * This constructor stores a local copy of its parameters and prepares
 * the layout of the dialog.
 *
 * @param parentWindow Parent window.
 * @param ph <code>caHandler<code> object that handles operations.
 ********************************************************************/
caSetThreshold (
	final Frame parentWindow
) {
	super(parentWindow, "Removing Points", true);
	setLayout(new GridLayout(3,2));
	lowerT_Text = new TextField("50");
	upperT_Text = new TextField("150");
	final Button setThresholdButton = new Button("Set Threshold");
	setThresholdButton.addActionListener(this);
	final Button resetThresholdButton = new Button("Reset Threshold");
	resetThresholdButton.addActionListener(this);
	final Button okButton = new Button("OK");
	okButton.addActionListener(this);
	final Button cancelButton = new Button("Cancel");
	cancelButton.addActionListener(this);
	add(lowerT_Text);
	add(upperT_Text);
	//add(separation1);
	add(setThresholdButton);
	//add(separation2);
	add(resetThresholdButton);
	//add(separation3);
	add(okButton);
	add(cancelButton);
	pack();
} /* end caSetThreshold */
} /* end class caSetThreshold */


class caBoundaryConditions
	extends
		Dialog
	implements
		ActionListener
	
 
{ /* begin class caBoundary */

/*....................................................................
	Private variables
....................................................................*/
caFile parent_dialog = null;
private TextField addP1_Text;
private TextField addP2_Text;
Checkbox setP1bond;
Checkbox setP2bond;
	public void actionPerformed(final ActionEvent ae) {
		if (ae.getActionCommand().equals("OK")) {
		int numPx1 = Double.valueOf(addP1_Text.getText()).intValue();
		int numPx2 = Double.valueOf(addP2_Text.getText()).intValue();
		parent_dialog.numP1 = numPx1;
		parent_dialog.numP2 = numPx2;
		
		parent_dialog.checkP1 = setP1bond.getState();
		parent_dialog.checkP2 = setP2bond.getState();
		boolean chkp1 = parent_dialog.checkP1;
		boolean chkp2 = parent_dialog.checkP2;
// Save the preferences in boundary.txt
		final String separator = System.getProperty("file.separator");
		String path = Menus.getPlugInsPath();
		String filename = "boundary.txt";
		try {
			final FileWriter fw = new FileWriter(path + filename);
			fw.write(numPx1+","+numPx2+","+chkp1+","+chkp2+".");
			fw.close();
		} catch (IOException e) {
			IJ.error("IOException exception");
		} catch (SecurityException e) {
			IJ.error("Security exception");
		}

// fine save
		setVisible(false);
		}
	} /* end actionPerformed */


caBoundaryConditions (
	final Frame parentWindow,
	caFile parent
) {
	super(parentWindow, "Boundary Conditions", true);
	parent_dialog = parent;
	setLayout(new GridLayout(3,2));
	addP1_Text = new TextField(Integer.toString(parent_dialog.numP1));
	addP2_Text = new TextField(Integer.toString(parent_dialog.numP2));
	setP1bond = new Checkbox("Use P1", parent_dialog.checkP1); 
	setP2bond = new Checkbox("Use P2", parent_dialog.checkP2);
	final Button okButton = new Button("OK");
	okButton.addActionListener(this);
	add(addP1_Text);
	add(addP2_Text);
	//add(separation1);
	add(setP1bond);
	//add(separation2);
	add(setP2bond);
	//add(separation3);
	add(okButton);
	pack();
	
	
} /* end caBoundary */

} /* end class caBoundary */






/*====================================================================
|	caFile
\===================================================================*/

/*********************************************************************
 * This class creates a dialog to store and retrieve points into and
 * from a text file, respectively.
 ********************************************************************/
class caFile
	extends
		Dialog
	implements
		ActionListener

{ /* begin class caFile */

private final CheckboxGroup choice = new CheckboxGroup();
private ImagePlus imp;
private caHandler ph;
public int numP1 = 50;
public int numP2 = 50;
public boolean checkP1 = true;
public boolean checkP2 = true;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 * This method processes the button actions.
 *
 * @param ae The expected actions are as follows:
 * <ul><li><code>Save as</code>: Save points into a text file;</li>
 * <li><code>Show</code>: Display the coordinates in ImageJ's window;</li>
 * <li><code>Open</code>: Retrieve points from a text file;</li>
 * <li><code>Cancel</code>: Do nothing.</li></ul>
 ********************************************************************/
public void actionPerformed (
	final ActionEvent ae
) {
// start load preferences

final Frame f = new Frame();
	try {
	final String separator = System.getProperty("file.separator");
	String path = Menus.getPlugInsPath();
	String filename = "boundary.txt";
//	System.out.println("loading prefs");
    final FileReader fr = new FileReader(path + filename);
    final BufferedReader br = new BufferedReader(fr);
    String line;
    String p1String;
    String p2String;
    String xString;
    String yString;
    int separatorIndex;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            separatorIndex = line.indexOf(",");
            if (separatorIndex == -1) {
                    fr.close();
                    IJ.error("Invalid SI");
                    separatorIndex = 2;
                    return;
            }
        //	System.out.println(separatorIndex);
            xString = line.substring(0, separatorIndex);
            xString = xString.trim();
            line = line.substring(separatorIndex+1);
            line = line.trim();
            separatorIndex = line.indexOf(",");				
            if (separatorIndex == -1) {
                separatorIndex = line.length();
            }
            yString = line.substring(0, separatorIndex);
            yString = yString.trim();
            line = line.substring(separatorIndex+1);
            separatorIndex = line.indexOf(",");
            p1String = line.substring(0, separatorIndex);
            line = line.substring(separatorIndex+1);
            separatorIndex = line.indexOf(".");
            p2String = line.substring(0, separatorIndex);
            if (p1String.equals("true")) checkP1 = true;
            else checkP1 = false;
            if (p2String.equals("true")) checkP2 = true;
            else checkP2 = false;	
            numP1 = Integer.parseInt(xString);
            numP2 = Integer.parseInt(yString);
            
        }
            fr.close();
        } catch (FileNotFoundException e) {
            //IJ.error("File not found exception");
        } catch (IOException e) {
            IJ.error("IOException exception");
        } catch (NumberFormatException e) {
            IJ.error("Number format exception");
        }
    // end load preferences
	
	this.setVisible(false);
	if (ae.getActionCommand().equals("Save as")) {
		final FileDialog fd = new FileDialog(f, "Point list", FileDialog.SAVE);
		final String path;
		String filename = imp.getTitle();
		final int dot = filename.lastIndexOf('.');
		if (dot == -1) {
			fd.setFile(filename + ".txt");
		}
		else {
			filename = filename.substring(0, dot);
			fd.setFile(filename + ".txt");
		}
		fd.setVisible(true);
		path = fd.getDirectory();
		filename = fd.getFile();
		if ((path == null) || (filename == null)) {
			return;
		}
		try {
			final FileWriter fw = new FileWriter(path + filename);
			final Vector list = ph.getPoints();
			Point p;
			String n;
			String x;
			String y;
			String z = "" + imp.getCurrentSlice();
			while (z.length() < 5) {
				z = " " + z;
			}
			fw.write("point     x     y slice\n");
			for (int k = 0; (k < list.size()); k++) {
				n = "" + k;
				while (n.length() < 5) {
					n = " " + n;
				}
				p = (Point)list.elementAt(k);
				x = "" + p.x;
				while (x.length() < 5) {
					x = " " + x;
				}
				y = "" + p.y;
				while (y.length() < 5) {
					y = " " + y;
				}
				fw.write(n + " " + x + " " + y + " " + z + "\n");
			}
			fw.close();
		} catch (IOException e) {
			IJ.error("IOException exception");
		} catch (SecurityException e) {
			IJ.error("Security exception");
		}
	}
	else if (ae.getActionCommand().equals("Show")) {
		final Vector list = ph.getPoints();
		Point p;
		String n;
		String x;
		String y;
		String z = "" + imp.getCurrentSlice();
		while (z.length() < 7) {
			z = " " + z;
		}
		IJ.getTextPanel().setFont(new Font("Monospaced", Font.PLAIN, 12));
		IJ.setColumnHeadings(" point\t      x\t      y\t  slice");
		for (int k = 0; (k < list.size()); k++) {
			n = "" + k;
			while (n.length() < 6) {
				n = " " + n;
			}
			p = (Point)list.elementAt(k);
			x = "" + p.x;
			while (x.length() < 7) {
				x = " " + x;
			}
			y = "" + p.y;
			while (y.length() < 7) {
				y = " " + y;
			}
			IJ.write(n + "\t" + x + "\t" + y + "\t" + z);
		}
	}
	else if (ae.getActionCommand().equals("Open")) {
		final FileDialog fd = new FileDialog(f, "Point list", FileDialog.LOAD);
		fd.setVisible(true);
		final String path = fd.getDirectory();
		final String filename = fd.getFile();
		if ((path == null) || (filename == null)) {
			return;
		}
		try {
			final FileReader fr = new FileReader(path + filename);
			final BufferedReader br = new BufferedReader(fr);
			ph.removePoints();
			String line;
			String pString;
			String xString;
			String yString;
			int separatorIndex;
			int x;
			int y;
			if ((line = br.readLine()) == null) {
				fr.close();
				return;
			}
			while ((line = br.readLine()) != null) {
				line = line.trim();
				separatorIndex = line.indexOf(' ');
				if (separatorIndex == -1) {
					fr.close();
					IJ.error("Invalid file");
					return;
				}
				line = line.substring(separatorIndex);
				line = line.trim();
				separatorIndex = line.indexOf(' ');
				if (separatorIndex == -1) {
					fr.close();
					IJ.error("Invalid file");
					return;
				}
				xString = line.substring(0, separatorIndex);
				xString = xString.trim();
				line = line.substring(separatorIndex);
				line = line.trim();
				separatorIndex = line.indexOf(' ');
				if (separatorIndex == -1) {
					separatorIndex = line.length();
				}
				yString = line.substring(0, separatorIndex);
				yString = yString.trim();
				x = Integer.parseInt(xString);
				y = Integer.parseInt(yString);
				ph.addPoint(x, y);
			}
			fr.close();
		} catch (FileNotFoundException e) {
			IJ.error("File not found exception");
		} catch (IOException e) {
			IJ.error("IOException exception");
		} catch (NumberFormatException e) {
			IJ.error("Number format exception");
		}
	}

/*********************************************************************
 *Boundary conditions: add or delete p1 and p2 from the list
 ********************************************************************/
 	else if (ae.getActionCommand().equals("Boundary Conditions")) {
	caBoundaryConditions BCDialog = new caBoundaryConditions(IJ.getInstance(), this);
	GUI.center(BCDialog);
	BCDialog.setVisible(true);
	BCDialog.dispose();
	this.setVisible(true);
	} /* end selection */	

	
/*********************************************************************
 *Inizio procedura calcolo con circle best fit NewtonRapson.
 ********************************************************************/
	else if (ae.getActionCommand().equals("Circle BestFit")) {
	final Vector list = ph.getPoints();
	if (list.size() != 5) {
		IJ.error("choose ONLY THE BASE LINE USING 2 POINTS and 3 FOR THE CIRCLE");
		return;
	}
	IJ.run("Find Edges", "slice");
	caSetThreshold ThresholdDialog = new caSetThreshold(IJ.getInstance());
	GUI.center(ThresholdDialog);
	ThresholdDialog.setVisible(true);
	ThresholdDialog.dispose();

			Point p1 = (Point) list.elementAt(0);
			Point p2 = (Point) list.elementAt(1);
			Point p3b = (Point) list.elementAt(2);
			Point p4 = (Point) list.elementAt(3);
			Point p5 = (Point) list.elementAt(4);
// circonferenza tra p3, p4 e p5 Utilizzata per avere una stima del
// centro a0 e b0 e per escludere eventuali punti spuri
			double[][] Acvalues = {
					{ 2 * p3b.x - 2 * p4.x, 2 * p3b.y - 2 * p4.y },
					{ 2 * p3b.x - 2 * p5.x, 2 * p3b.y - 2 * p5.y } };
			double[][] Scvalues = {
					{ p3b.x * p3b.x + p3b.y * p3b.y - p4.x * p4.x - p4.y * p4.y },
					{ p3b.x * p3b.x + p3b.y * p3b.y - p5.x * p5.x - p5.y * p5.y } };
			Matrix Ac = new Matrix(Acvalues);
			Matrix Sc = new Matrix(Scvalues);
			Matrix Xc = Ac.solve(Sc);
			double ragc = Math.sqrt(p4.x * p4.x + p4.y * p4.y - 2 * p4.x
					* Xc.get(0, 0) - 2 * p4.y * Xc.get(1, 0) + Xc.get(0, 0)
					* Xc.get(0, 0) + Xc.get(1, 0) * Xc.get(1, 0));

			final Vector drop = new Vector(0, 16);
			final Vector dropred = new Vector(0, 16);

// calcolo retta di base tra p1 e p2: m X + q = y
	double dify = p1.y - p2.y;
	double difx = p1.x - p2.x;
	double m = dify/difx;
	double q = p1.y- m*p1.x;	
	
//analisi immagine e cattura punti	
	ImageProcessor ip = imp.getProcessor();
		String imgFileName = imp.getTitle();
		int width = imp.getWidth();
		int height = imp.getHeight();
		double background = 255;
		String bg;
		if (ip instanceof ColorProcessor) {
			int c = ip.getPixel(0,0);
			int r = (c&0xff0000)>>16;
			int g = (c&0xff00)>>8;
			int b = c&0xff;
			bg = r+","+g+","+b;
		} else {
			if ((int)background==background)
				bg = IJ.d2s(background,0);
			else
				bg = ""+background;
		}
		imp.killRoi();

		int count = 0;
		String ls = System.getProperty("line.separator");
		float v;
		double maxy = 0;
		int c,r,g,b;
		int xt=0;
		int xm=0;
		int counter=1;
		if (p1.y >= p2.y) {
		maxy = p1.y; 
		} else {
		maxy = p2.y; 
		}
		int type = imp.getType();
		for (int y=height-2; y>= maxy; y--) {
			for (int x=10; x<width-10; x++) {
				v = ip.getPixelValue(x,y);
				if (v!=background) {
					if (v==ip.getPixelValue(x+1,y)) {
						xt=xt+x;
						counter=counter+1;
						}
					else 	{
						if (counter==1) {
						// eliminazione punti singoli xm=x;
						}
					      	else {
							xt=xt+x;
							xm=xt/counter;
							xt=0;
							counter=1;
							Point pd= new Point (xm, y);
// Accetto solo i punti distanti dal centro uguale al raggio + - 25 pixel per comodità il vettore finale si chiamerà ancora dropred
							double distance = Math.sqrt(pd.x * pd.x + pd.y * pd.y - 2* pd.x * Xc.get(0,0) - 2* pd.y * Xc.get(1,0) + Xc.get(0,0)*Xc.get(0,0) + Xc.get(1,0)*Xc.get(1,0));
							if ((Math.abs(distance - ragc)) < 25) {
								dropred.addElement(pd);
							}
							}
						}
						}
			}

		}
		IJ.showProgress(1.0);
// aggiungo n copie di ciascuno dei punti base p1 e p2
if (this.checkP1) {	
	for (int k=0; k<this.numP1; k++) {
		Point pnew1=new Point(p1);
		dropred.addElement(pnew1);
	}
}
if (this.checkP2) {
	for (int k=0; k<this.numP2; k++) {			
		Point pnew2=new Point(p2);
		dropred.addElement(pnew2);
	}
}	
	double npfinale = dropred.size();
			
//inizio curcle best fit: a,b centro supposto
       final Vector fi = new Vector(0, 16);
	int a0= (int)Xc.get(0,0);
	int b0= (int)Xc.get(1,0);
	int ai = a0;
	int bi = b0;
        double naf = 0;
	double fijt = 0;
// nr numero massimo di iterazioni fatte: normalmente converge in meno di 10 
	for (int nr=0; nr<= 99; nr++) {
		double fijxt = 0;
		double fijyt = 0;
		double laa1t = 0;
		double laa2t = 0;
		double lab1t = 0;
		double lab2t = 0;
		double lbb2t = 0;
		fijt = 0;		
		for (int k=0; k< npfinale; k++) {
			Point pp = (Point) dropred.elementAt(k);
			double fijx =(ai- pp.x);
			double fijy =(bi- pp.y);
			double fij = Math.sqrt(Math.pow(fijx,2) + Math.pow(fijy,2));

		if (fij == 0)  {
		IJ.error("ERROR: Fij Parameter equal to ZERO");
		return;
		}

			double laa1 = fijx/fij;
			double laa2 = Math.pow(fijy,2)/Math.pow(fij,3);
			double lab1 = fijy/fij;
			double lab2 = laa1*lab1/fij;
			double lbb2 = Math.pow(fijx,2)/Math.pow(fij,3);
			fijxt = fijxt + fijx;
			fijyt = fijyt + fijy;
			fijt = fijt + fij;
			laa1t = laa1t + laa1;
			laa2t = laa2t + laa2;
			lab1t = lab1t + lab1;
			lab2t = lab2t + lab2;
			lbb2t = lbb2t + lbb2;
		}		
		
		double laa = npfinale - (1/npfinale)*Math.pow(laa1t,2) - (1/npfinale)*fijt*laa2t;
		double lab = (-1/npfinale)*lab1t*laa1t + (1/npfinale)*fijt*lab2t;
		double lbb = npfinale - (1/npfinale)*Math.pow(lab1t,2) - (1/npfinale)*fijt*lbb2t;
		double f1 = fijxt - (1/npfinale)*fijt*laa1t;
		double f2 = fijyt - (1/npfinale)*fijt*lab1t;
		double fract = (Math.pow(lab,2) - laa*lbb);

		if (fract == 0)  {
		IJ.error("ERROR: fract Parameter equal to ZERO");
		return;
		}

		double deltaai = (lbb*f1 - lab*f2)/fract;
		double deltabi = (laa*f2 - lab*f1)/fract;
		int deltaa = (int)deltaai;
		int deltab = (int)deltabi;
		ai = ai + deltaa;
		bi = bi + deltab;
		if (deltaa==0 && deltab==0) {
                        naf = nr;
			nr=1000;
			}
		//if spostamento centro < 1 esci
	}
		double rag = ((1/npfinale)*fijt);
		
/* calcolo GEOMETRICAL STANDARD DEVIATION of the best fit function */
		double scrt =0;
		double scrti =0;
		for (int k=0; k< npfinale; k++) {
			Point pp = (Point) dropred.elementAt(k);
			double fijx =(ai- pp.x);
			double fijy =(bi- pp.y);
		scrti = Math.pow(Math.sqrt(Math.pow(fijx,2) + Math.pow(fijy,2)) - rag,2);
		scrt = scrt + scrti;
		}
		double stdev = Math.sqrt(scrt/(npfinale-1));
/* calcolo punti intersezione circonferenza e retta di base   */
		double b1 = 2*(q*m - ai - m*bi)/(1+m*m);
		double b2 = (ai*ai +q*q + bi*bi -2*bi*q - rag*rag)/(1+m*m);
		double delta = b1*b1 - 4*b2;
		if (delta < 0) {
			IJ.showMessage("The DROP doesn't touch the selected base line");
			return;
			}
		if (delta == 0) {
			IJ.showMessage("The Contact ANGLE of this DROP on the base line is 180 degrees");
			return;
			}		
		double xb1 = ((-b1+Math.sqrt(delta))/2);
		double yb1 = (m*xb1 +q);
		double xb2 = ((-b1-Math.sqrt(delta))/2);
		double yb2 = (m*xb2 +q);
		int xb1i = (int)xb1;
		int yb1i = (int)yb1;
		int xb2i = (int)xb2;
		int yb2i = (int)yb2;
		//IJ.run("Revert");
		IJ.run("Line Width...", "line=3");
		IJ.makeLine(xb1i, yb1i, xb2i, yb2i);
		IJ.run("Draw","slice");
		double wt = Math.sqrt((xb2-xb1)*(xb2-xb1) + (yb2-yb1)*(yb2-yb1));

/* calcolo culmine: distanza centro - base + raggio punto centrale*/
		double cbt =  -(m*ai - bi + q)/Math.sqrt(m*m + 1);
		double ht = rag + cbt;
		double exsol=2*ht/wt;
		double thetac = 2*Math.atan(exsol);
		double area = 2*Math.PI*rag*ht;
		double volume = ht*ht*(3*rag - ht)*Math.PI/3;
/* Error propagation */
		double error = 4*Math.sqrt(2)*stdev/((1+exsol)*wt);
		
		
		IJ.run("Set Measurements...", "display decimal=14");
		
		Roi roi = imp.getRoi();
		Analyzer a = new Analyzer();
		ImageStatistics stats = imp.getStatistics(Analyzer.getMeasurements());
		a.saveResults(stats, roi); // store in system results table
		ResultsTable rt =Analyzer.getResultsTable(); // get the system results table
		rt.addLabel("File Name", imgFileName);	
		rt.addValue("Frame", imp.getCurrentSlice());
		rt.addValue("Theta", (Math.rint(thetac*1800/Math.PI)/10));
		rt.addValue("Uncertainty", (Math.rint(error*1800/Math.PI)/10));
		rt.addValue("Circle StDev", stdev);
		rt.addValue("Radius", Math.rint(rag*100)/100);
                rt.addValue("X Centre", ai);
                rt.addValue("Y Centre", bi);
                rt.addValue("Iterations:", naf);
                rt.addValue("Points:", npfinale);
		rt.addValue("Volume", Math.rint(volume*100)/100);
		rt.addValue("Area Sup", Math.rint(area*100)/100);
		rt.addValue("Base", Math.rint(wt*100)/100);
		rt.addValue("Height", Math.rint(ht*100)/100);
		a.displayResults();
		a.updateHeadings();
		

		double ovalxb = (ai - rag);
		double ovalyb = (bi - rag);
		IJ.run("Line Width...", "line=3");

		IJ.makeOval((int)(ovalxb), (int)(ovalyb), (int)(2*rag), (int)(2*rag));
		IJ.run("Draw","slice");
		}
		

/*********************************************************************
 *Inizio procedura calcolo con approssimazione ellisse ax2+bxy+cy2+dx+ey+f=0.
 ********************************************************************/	
 	else if (ae.getActionCommand().equals("Ellipse Bestfit")) {	
	
	final Vector list = ph.getPoints();
	if (list.size() != 5)  {
        IJ.error("choose ONLY THE BASE LINE USING 2 POINTS and 3 FOR THE DROP EDGE");
			return;
		}
	IJ.run("Find Edges","slice");
	caSetThreshold ThresholdDialog = new caSetThreshold(IJ.getInstance());
	GUI.center(ThresholdDialog);
	ThresholdDialog.setVisible(true);
	ThresholdDialog.dispose();

		Point p1 = (Point) list.elementAt(0);
		Point p2 = (Point) list.elementAt(1);
 		Point p3b = (Point) list.elementAt(2);
		Point p4 = (Point) list.elementAt(3);
		Point p5 = (Point) list.elementAt(4);
// circonferenza tra p3, p4 e p5 Utilizzata per avere una stima del centro a0 e b0 e per escludere eventuali punti spuri
		double[][] Acvalues = {{2*p3b.x-2*p4.x, 2*p3b.y-2*p4.y},{2*p3b.x-2*p5.x, 2*p3b.y-2*p5.y}}; 
		double[][] Scvalues = {{p3b.x*p3b.x+p3b.y*p3b.y-p4.x*p4.x-p4.y*p4.y},{p3b.x*p3b.x+p3b.y*p3b.y-p5.x*p5.x-p5.y*p5.y}};
		Matrix Ac = new Matrix(Acvalues);
		Matrix Sc = new Matrix(Scvalues);
		Matrix Xc = Ac.solve(Sc);
		double ragc = Math.sqrt(p4.x * p4.x + p4.y * p4.y - 2* p4.x * Xc.get(0,0) - 2* p4.y * Xc.get(1,0) + Xc.get(0,0)*Xc.get(0,0) + Xc.get(1,0)*Xc.get(1,0));
       
        final Vector drop = new Vector(0, 16);
	final Vector dropred = new Vector(0, 16);

// calcolo retta di base tra p1 e p2: y = m X + q 
	double dify = p1.y - p2.y;
	double difx = p1.x - p2.x;
	double m = dify/difx;
	double q = p1.y- m*p1.x;	
	
//analisi immagine e cattura punti	
	ImageProcessor ip = imp.getProcessor();
		String imgFileName = imp.getTitle();
		int width = imp.getWidth();
		int height = imp.getHeight();
		double background = 255;
		String bg;
		if (ip instanceof ColorProcessor) {
			int c = ip.getPixel(0,0);
			int r = (c&0xff0000)>>16;
			int g = (c&0xff00)>>8;
			int b = c&0xff;
			bg = r+","+g+","+b;
		} else {
			if ((int)background==background)
				bg = IJ.d2s(background,0);
			else
				bg = ""+background;
		}
		imp.killRoi();

		int count = 0;
		String ls = System.getProperty("line.separator");
		float v;
		double maxy = 0;
		int c,r,g,b;
		int xt=0;
		int xm=0;
		int counter=1;
		if (p1.y >= p2.y) {
		maxy = p1.y; 
		} else {
		maxy = p2.y; 
		}
		int type = imp.getType();
		for (int y=height-2; y>= maxy; y--) {
			for (int x=10; x<width-10; x++) {
				v = ip.getPixelValue(x,y);
				if (v!=background) {
					if (v==ip.getPixelValue(x+1,y)) {
						xt=xt+x;
						counter=counter+1;
						}
					else 	{
						if (counter==1) {
						// eliminazione punti singoli xm=x;
						}
					      	else {
							xt=xt+x;
							xm=xt/counter;
							xt=0;
							counter=1;
							Point pd= new Point (xm, y);
// Accetto solo i punti distanti dal centro uguale al raggio + - 25 pixel per comodità il vettore finale si chiamerà ancora dropred
							double distance = Math.sqrt(pd.x * pd.x + pd.y * pd.y - 2* pd.x * Xc.get(0,0) - 2* pd.y * Xc.get(1,0) + Xc.get(0,0)*Xc.get(0,0) + Xc.get(1,0)*Xc.get(1,0));
							if ((Math.abs(distance - ragc)) < 25) {
								dropred.addElement(pd);
							}
							}
						}
						}
			}

		}
		IJ.showProgress(1.0);
		int npunti = dropred.size();
		if (npunti < 10) IJ.error("WARNING: too few points in the profile (less than 10)");
		
// aggiungo n copie di ciascuno dei punti base p1 e p2
if (this.checkP1) {	
	for (int k=0; k<this.numP1; k++) {
		Point pnew1=new Point(p1);
		dropred.addElement(pnew1);
	}
}
if (this.checkP2) {
	for (int k=0; k<this.numP2; k++) {			
		Point pnew2=new Point(p2);
		dropred.addElement(pnew2);
	}
}
	double npfinale = dropred.size();

			
//inizio best fit ellisse: calcolo delle matrici s1, s2 ed s3
	double S111t = 0;
	double S112t = 0;
	double S113t = 0;
	double S123t = 0;
	double S133t = 0;

	double S211t = 0;
	double S212t = 0;
	double S213t = 0;
	double S222t = 0;
	double S223t = 0;
	double S232t = 0;
	double S233t = 0;

	double S311t = 0;
	double S312t = 0;
	double S313t = 0;
	double S322t = 0;
	double S323t = 0;
	double S333t = 0;	

	for (int k=0; k< npfinale; k++) {
		Point pp = (Point) dropred.elementAt(k);
		double S111 = Math.pow(pp.x,4);
		double S112 = Math.pow(pp.x,3) * pp.y;
		double S113 = Math.pow(pp.x,2) * Math.pow(pp.y,2);
		double S123 = Math.pow(pp.x,1) * Math.pow(pp.y,3);
		double S133 = Math.pow(pp.y,4);
		
		double S211 = Math.pow(pp.x,3);
		double S212 = Math.pow(pp.x,2) * Math.pow(pp.y,1);
		double S213 = Math.pow(pp.x,2);
		double S222 = Math.pow(pp.x,1) * Math.pow(pp.y,2);
		double S223 = Math.pow(pp.x,1) * Math.pow(pp.y,1);
		double S232 = Math.pow(pp.y,3);
		double S233 = Math.pow(pp.y,2);
		
		S111t = S111t + S111;
		S112t = S112t + S112;
		S113t = S113t + S113;
		S123t = S123t + S123;
		S133t = S133t + S133;
		
		S211t = S211t + S211;
		S212t = S212t + S212;
		S213t = S213t + S213;
		S222t = S222t + S222;
		S223t = S223t + S223;
		S232t = S232t + S232;
		S233t = S233t + S233;
		
		S311t = S311t + S213;
		S312t = S312t + S223;
		S313t = S313t + pp.x;
		S322t = S322t + S233;
		S323t = S323t + pp.y;
		S333t = npfinale;
	}

	double[][] S1lin = {{S111t, S112t, S113t},{S112t, S113t, S123t},{S113t, S123t, S133t}};
	Matrix S1 = new Matrix(S1lin);
	
	double[][] S2lin = {{S211t, S212t, S213t},{S212t, S222t, S223t},{S222t, S232t, S233t}};
	Matrix S2 = new Matrix(S2lin);	
	
	double[][] S3lin = {{S311t, S312t, S313t},{S312t, S322t, S323t},{S313t, S323t, S333t}};
	Matrix S3 = new Matrix(S3lin);

	double s3det = S3.det();
		if (s3det == 0)  {
		IJ.error("ERROR: S3 Matrix is not invertible");
		return;
		}
		
	
	double[][] C1invlin = {{0, 0, 0.5},{0, -1, 0},{0.5, 0, 0}};
	Matrix C1inv = new Matrix(C1invlin);

	Matrix S2t = S2.transpose();
	Matrix S3i = S3.inverse();
	
	Matrix S4 = S3i.times(S2t);
	Matrix S5 = S2.times(S4);
	Matrix S6 = S1.minus(S5);
	Matrix M = C1inv.times(S6);
	
	EigenvalueDecomposition Emme = M.eig();
	Matrix Eival = Emme.getD();
	Matrix Eivec = Emme.getV();
	
	double alpha1 = Eival.get(0,0);
	double alpha2 = Eival.get(1,1);
	double alpha3 = Eival.get(2,2);
	
	int evpos =0;
	for (int nr=0; nr<= 2; nr++) {
		if (Eival.get(nr,nr) > 0)  {
		evpos = nr;
		}		
	}
	Matrix abc = Eivec.getMatrix(0,2,evpos,evpos);
	
	Matrix def = S4.times(abc).times(-1);
	double acoef = abc.get(0,0);
	double bcoef = abc.get(1,0);
	double ccoef = abc.get(2,0);
	double dcoef = def.get(0,0);
	double ecoef = def.get(1,0);
	double fcoef = def.get(2,0);
	
	double verifica = 4*acoef*ccoef -bcoef*bcoef;

//	Matrice dei coefficienti	
	double[][] abcdeflin = {{acoef, bcoef/2, dcoef/2},{bcoef/2, ccoef, ecoef/2},{dcoef/2, ecoef/2, fcoef}};
	Matrix abcdef = new Matrix(abcdeflin);
	double abcdefdet = abcdef.det();
	double[][] a33lin = {{acoef, bcoef/2},{bcoef/2, ccoef}};
	Matrix a33 = new Matrix(a33lin);
	double a33det = a33.det();	
//	Calcolo centro ellisse	
	double[][] Sa2lin = {{-dcoef/2},{-ecoef/2}};
	Matrix Sa2 = new Matrix(Sa2lin);
	Matrix Xcentre = a33.solve(Sa2);
	double centrex= Xcentre.get(0,0);
	double centrey= Xcentre.get(1,0);
	
//	Calcolo i semi assi
	EigenvalueDecomposition Ava33 = a33.eig();
	Matrix Eivala33 = Ava33.getD();
	double beta1 = Eivala33.get(0,0);
	double beta2 = Eivala33.get(1,1);
	double semiasse2 = Math.sqrt((Math.abs(abcdefdet))/((Math.abs(beta1)*a33det)));
	double semiasse1 = Math.sqrt((Math.abs(abcdefdet))/((Math.abs(beta2)*a33det)));
	double eccentr = semiasse1/semiasse2;
	if (eccentr > 1) eccentr=1/eccentr;

//      rette assi alfaA(x - centrex) - betaA(y - centrey) = 0
//	e betaA(x - centrex) + alfaA(y - centrey) = 0
	double alfaA = bcoef;
	double betaA = acoef - ccoef + Math.sqrt(Math.pow((acoef-ccoef),2) + Math.pow(bcoef,2));
	double masse1 = alfaA/betaA;
	double qasse1 = ((betaA*centrey)-(alfaA*centrex))/betaA;
	double masse2 = -betaA/alfaA;
	double qasse2 = ((betaA*centrex)+(alfaA*centrey))/alfaA;
	double thetaas1 = (Math.atan(masse1));


	
// 	Punti intersezione con retta di base
	double c1 = acoef + (bcoef*m) + ccoef*Math.pow(m,2);
	double c2 = (bcoef*q) + (2*ccoef*m*q) + dcoef + (ecoef*m);
	double c3 = ccoef*Math.pow(q,2) + (ecoef*q) + fcoef;
	double delta = Math.pow(c2,2) - (4*c1*c3);
		if (delta < 0) {
		IJ.showMessage("The DROP doesn't touch the selected base line");
		return;
		}
		if (delta == 0) {
		IJ.showMessage("The Contact ANGLE of this DROP on the base line is 180 degrees");
		return;
		}		
		
	double xb1 = ((-c2+Math.sqrt(delta))/(2*c1));
	double yb1 = (m*xb1) +q;
	double xb2 = ((-c2-Math.sqrt(delta))/(2*c1));
	double yb2 = (m*xb2) +q;
//      Impongo che xb2 sia left, xb1 right
        double xbtest, ybtest;
        if (xb1 < xb2) {
		xbtest = xb1;
                ybtest = yb1;
                xb1 = xb2;
                yb1 = yb2;
                xb2 = xbtest;
                yb2 = ybtest;
	}
        
	int xb1i = (int)xb1;
	int yb1i = (int)yb1;
	int xb2i = (int)xb2;
	int yb2i = (int)yb2;
	//IJ.run("Revert");
	IJ.run("Line Width...", "line=2");
	IJ.makeLine(xb1i, yb1i, xb2i, yb2i);
	IJ.run("Draw","slice");
	double wt = Math.sqrt((xb2-xb1)*(xb2-xb1) + (yb2-yb1)*(yb2-yb1));
	
//	Calcolo tangenze nei punti di intersezione y = mtg x + qtg ; 
	double[][] point1lin = {{xb1, yb1, 1}};
	Matrix point1 = new Matrix(point1lin);	
	double[][] point2lin = {{xb2, yb2, 1}};
	Matrix point2 = new Matrix(point2lin);
	Matrix tangc1 = point1.times(abcdef);
	Matrix tangc2 = point2.times(abcdef);
	double mtg1 = -tangc1.get(0,0)/tangc1.get(0,1);
	double qtg1 = -tangc1.get(0,2)/tangc1.get(0,1);
	double mtg2 = -tangc2.get(0,0)/tangc2.get(0,1);
	double qtg2 = -tangc2.get(0,2)/tangc2.get(0,1);
//      calcolo punti sulla cornice
	int xb3i =0;
        int yb3i =0;
        int xb4i =width;
        int yb4i =0;
        int xb5i =0;
        int yb5i =0;
        int xb6i =width;
        int yb6i =0;
        yb5i = (int)(qtg2);
        if (yb5i < 0) {
		yb5i = 0;
                xb5i = (int)(-qtg2/mtg2);
	}
        yb6i = (int)((mtg2*xb6i)+ qtg2);
        if (yb6i < 0) {
		yb6i = 0;
                xb6i = (int)(-qtg2/mtg2);
	}
        yb3i = (int)qtg1;
        if (yb3i < 0) {
		yb3i = 0;
                xb3i = (int)(-qtg1/mtg1);
	}
	yb4i = (int)((mtg1*xb4i)+ qtg1);
        if (yb4i < 0) {
		yb4i = 0;
                xb4i = (int)(-qtg1/mtg1);
	}
     	IJ.makeLine(xb3i, yb3i, xb4i, yb4i);
	IJ.run("Draw","slice");
	IJ.makeLine(xb5i, yb5i, xb6i, yb6i);
	IJ.run("Draw","slice");
//	IJ.write("coefficienti tg1 =" + mtg1 + " " + qtg1);
//	IJ.write("coefficienti tg2 =" + mtg2 + " " + qtg2);
        double thetal = Math.rint((Math.atan(mtg2)*1800/Math.PI))/10;
        double thetar = Math.rint((Math.atan(mtg1)*1800/Math.PI))/10;
	double thetapend = (Math.rint(Math.atan(m)*1800/Math.PI)/10);
        double thetargradi = thetar;
        double thetalgradi = thetal;
        if (thetal < 0) {
                thetalgradi = 180 + thetal;
                }

        if (thetar < 0) {
                thetargradi = -thetar;
                }
        else {
                thetargradi = 180-thetar;
                }                
		thetalgradi = thetalgradi - thetapend;
                thetargradi = thetargradi + thetapend;
                
                double thetaaverage = (thetalgradi + thetargradi)/2;
		
//	Calcolo errore fit ellisse GEOMETRICAL ST DEV
	double scrte =0;
	double scrtie =0;	
	for (int k=0; k< npfinale; k++) {
		Point pp = (Point) dropred.elementAt(k);
		scrte = scrte + Math.abs(acoef*Math.pow(pp.x,2) + bcoef* pp.x * pp.y + ccoef * Math.pow(pp.y,2) + dcoef*pp.x + ecoef*pp.y + fcoef);
	}
	double stdeve = Math.sqrt(scrte/(npfinale-1));
		IJ.run("Set Measurements...", "display decimal=14");
		
		Roi roi = imp.getRoi();
		Analyzer a = new Analyzer();
		ImageStatistics stats = imp.getStatistics(Analyzer.getMeasurements());
		a.saveResults(stats, roi); // store in system results table
		ResultsTable rt =Analyzer.getResultsTable(); // get the system results table
		rt.addLabel("File Name", imgFileName);
		rt.addValue("Frame", imp.getCurrentSlice());
		rt.addValue("Theta Left", ((Math.rint(thetalgradi*10))/10));
		rt.addValue("Theta Right", ((Math.rint(thetargradi*10))/10));
		rt.addValue("Theta", (Math.rint(thetaaverage*10))/10);
//		rt.addValue("Theta Asse1", thetaas1);
//		rt.addValue("Theta Asse2", thetaas2);		
//		rt.addValue("Theta Pend", thetapend);
		rt.addValue("e", Math.rint(eccentr*100)/100);
		rt.addValue("Ellipse StDev", stdeve);
//		rt.addValue("Base", Math.rint(wt*100)/100);
		a.displayResults();
		a.updateHeadings();

/* Disegno Ovale*/
		int NUMPOLY = 300;
		int[] xPoints, yPoints;
		xPoints = new int[NUMPOLY];
		yPoints = new int[NUMPOLY];
		double hTheta = 2*Math.PI/NUMPOLY;
		double cosasse1 = Math.cos(thetaas1);
		double sinasse1 = Math.sin(thetaas1);
		for (int i = 0; i < NUMPOLY; i++){
			double Rtheta = i*hTheta;
			double cos = Math.cos(Rtheta);
			double sin = Math.sin(Rtheta);
			double xor = semiasse1*cos;
			double yor = semiasse2*sin;
			xPoints[i] = (int)Math.round(centrex + (xor*cosasse1) - (yor*sinasse1));
			yPoints[i] = (int)Math.round(centrey + (xor*sinasse1) + (yor*cosasse1));
		}
		PolygonRoi proi = new PolygonRoi(xPoints, yPoints,NUMPOLY,Roi.TRACED_ROI);
		imp.setRoi(proi);
		IJ.run("Draw","slice");
//Intersezione con l'asse maggiore ed asse minore
	 	c1 = acoef + (bcoef*masse1) + ccoef*Math.pow(masse1,2);
		c2 = (bcoef*qasse1) + (2*ccoef*masse1*qasse1) + dcoef + (ecoef*masse1);
		c3 = ccoef*Math.pow(qasse1,2) + (ecoef*qasse1) + fcoef;
		delta = Math.pow(c2,2) - (4*c1*c3);
		
		xb1 = ((-c2+Math.sqrt(delta))/(2*c1));
		yb1 = (masse1*xb1) +qasse1;
		xb2 = ((-c2-Math.sqrt(delta))/(2*c1));
		yb2 = (masse1*xb2) +qasse1;
			xb1i = (int)xb1;
			yb1i = (int)yb1;
			xb2i = (int)xb2;
			yb2i = (int)yb2;
		if (yb1i < 0) {
			yb1i = 0;
			xb1i = (int)(-qasse1/masse1);
		}
		if (yb2i < 0) {
			yb2i = 0;
			xb2i = (int)(-qasse1/masse1);
		}
	IJ.makeLine(xb1i, yb1i, xb2i, yb2i);
	IJ.run("Draw","slice");
	
	 	c1 = acoef + (bcoef*masse2) + ccoef*Math.pow(masse2,2);
		c2 = (bcoef*qasse2) + (2*ccoef*masse2*qasse2) + dcoef + (ecoef*masse2);
		c3 = ccoef*Math.pow(qasse2,2) + (ecoef*qasse2) + fcoef;
		delta = Math.pow(c2,2) - (4*c1*c3);
		
		xb1 = ((-c2+Math.sqrt(delta))/(2*c1));
		yb1 = (masse2*xb1) +qasse2;
		xb2 = ((-c2-Math.sqrt(delta))/(2*c1));
		yb2 = (masse2*xb2) +qasse2;
			xb1i = (int)xb1;
			yb1i = (int)yb1;
			xb2i = (int)xb2;
			yb2i = (int)yb2;
		if (yb1i < 0) {
			yb1i = 0;
			xb1i = (int)(-qasse2/masse2);
		}
		if (yb2i < 0) {
			yb2i = 0;
			xb2i = (int)(-qasse2/masse2);
		}
	IJ.makeLine(xb1i, yb1i, xb2i, yb2i);
	IJ.run("Draw","slice");	
		}

/*********************************************************************
 *Inizio procedura calcolo both bestfits..
 ********************************************************************/	
 	else if (ae.getActionCommand().equals("Both Bestfits")) {	
	
	final Vector list = ph.getPoints();
	if (list.size() != 5)  {
        IJ.error("choose ONLY THE BASE LINE USING 2 POINTS and 3 FOR THE DROP EDGE");
			return;
		}
	IJ.run("Find Edges","slice");
	caSetThreshold ThresholdDialog = new caSetThreshold(IJ.getInstance());
	ThresholdDialog.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "OK"));
	//ThresholdDialog.setVisible(true);
	ThresholdDialog.dispose();

		Point p1 = (Point) list.elementAt(0);
		Point p2 = (Point) list.elementAt(1);
 		Point p3b = (Point) list.elementAt(2);
		Point p4 = (Point) list.elementAt(3);
		Point p5 = (Point) list.elementAt(4);
// circonferenza tra p3, p4 e p5 Utilizzata per avere una stima del centro a0 e b0 e per escludere eventuali punti spuri
		double[][] Acvalues = {{2*p3b.x-2*p4.x, 2*p3b.y-2*p4.y},{2*p3b.x-2*p5.x, 2*p3b.y-2*p5.y}}; 
		double[][] Scvalues = {{p3b.x*p3b.x+p3b.y*p3b.y-p4.x*p4.x-p4.y*p4.y},{p3b.x*p3b.x+p3b.y*p3b.y-p5.x*p5.x-p5.y*p5.y}};
		Matrix Ac = new Matrix(Acvalues);
		Matrix Sc = new Matrix(Scvalues);
		Matrix Xc = Ac.solve(Sc);
		double ragc = Math.sqrt(p4.x * p4.x + p4.y * p4.y - 2* p4.x * Xc.get(0,0) - 2* p4.y * Xc.get(1,0) + Xc.get(0,0)*Xc.get(0,0) + Xc.get(1,0)*Xc.get(1,0));
       
        final Vector drop = new Vector(0, 16);
	final Vector dropred = new Vector(0, 16);

// calcolo retta di base tra p1 e p2: y = m X + q 
	double dify = p1.y - p2.y;
	double difx = p1.x - p2.x;
	double m = dify/difx;
	double q = p1.y- m*p1.x;	
	
//analisi immagine e cattura punti	
	ImageProcessor ip = imp.getProcessor();
		String imgFileName = imp.getTitle();
		int width = imp.getWidth();
		int height = imp.getHeight();
		double background = 255;
		String bg;
		if (ip instanceof ColorProcessor) {
			int c = ip.getPixel(0,0);
			int r = (c&0xff0000)>>16;
			int g = (c&0xff00)>>8;
			int b = c&0xff;
			bg = r+","+g+","+b;
		} else {
			if ((int)background==background)
				bg = IJ.d2s(background,0);
			else
				bg = ""+background;
		}
		imp.killRoi();

		int count = 0;
		String ls = System.getProperty("line.separator");
		float v;
		double maxy = 0;
		int c,r,g,b;
		int xt=0;
		int xm=0;
		int counter=1;
		if (p1.y >= p2.y) {
		maxy = p1.y; 
		} else {
		maxy = p2.y; 
		}
		int type = imp.getType();
		for (int y=height-2; y>= maxy; y--) {
			for (int x=10; x<width-10; x++) {
				v = ip.getPixelValue(x,y);
				if (v!=background) {
					if (v==ip.getPixelValue(x+1,y)) {
						xt=xt+x;
						counter=counter+1;
						}
					else 	{
						if (counter==1) {
						// eliminazione punti singoli xm=x;
						}
					      	else {
							xt=xt+x;
							xm=xt/counter;
							xt=0;
							counter=1;
							Point pd= new Point (xm, y);
// Accetto solo i punti distanti dal centro uguale al raggio + - 25 pixel per comodità il vettore finale si chiamerà ancora dropred
							double distance = Math.sqrt(pd.x * pd.x + pd.y * pd.y - 2* pd.x * Xc.get(0,0) - 2* pd.y * Xc.get(1,0) + Xc.get(0,0)*Xc.get(0,0) + Xc.get(1,0)*Xc.get(1,0));
							if ((Math.abs(distance - ragc)) < 25) {
								dropred.addElement(pd);
							}
							}
						}
						}
			}

		}
	IJ.showProgress(1.0);
	int npunti = dropred.size();
	if (npunti<10) IJ.error("WARNING: too few points in the profile (less than 10)");

// aggiungo n copie di ciascuno dei punti base p1 e p2
if (this.checkP1) {	
	for (int k=0; k<this.numP1; k++) {
		Point pnew1=new Point(p1);
		dropred.addElement(pnew1);
	}
}
if (this.checkP2) {
	for (int k=0; k<this.numP2; k++) {			
		Point pnew2=new Point(p2);
		dropred.addElement(pnew2);
	}
}
	double npfinale = dropred.size();

			
//inizio best fit ellisse: calcolo delle matrici s1, s2 ed s3

	double S111t = 0;
	double S112t = 0;
	double S113t = 0;
	double S123t = 0;
	double S133t = 0;

	double S211t = 0;
	double S212t = 0;
	double S213t = 0;
	double S222t = 0;
	double S223t = 0;
	double S232t = 0;
	double S233t = 0;

	double S311t = 0;
	double S312t = 0;
	double S313t = 0;
	double S322t = 0;
	double S323t = 0;
	double S333t = 0;	

	for (int k=0; k< npfinale; k++) {
		Point pp = (Point) dropred.elementAt(k);
		double S111 = Math.pow(pp.x,4);
		double S112 = Math.pow(pp.x,3) * pp.y;
		double S113 = Math.pow(pp.x,2) * Math.pow(pp.y,2);
		double S123 = Math.pow(pp.x,1) * Math.pow(pp.y,3);
		double S133 = Math.pow(pp.y,4);
		
		double S211 = Math.pow(pp.x,3);
		double S212 = Math.pow(pp.x,2) * Math.pow(pp.y,1);
		double S213 = Math.pow(pp.x,2);
		double S222 = Math.pow(pp.x,1) * Math.pow(pp.y,2);
		double S223 = Math.pow(pp.x,1) * Math.pow(pp.y,1);
		double S232 = Math.pow(pp.y,3);
		double S233 = Math.pow(pp.y,2);
		
		S111t = S111t + S111;
		S112t = S112t + S112;
		S113t = S113t + S113;
		S123t = S123t + S123;
		S133t = S133t + S133;
		
		S211t = S211t + S211;
		S212t = S212t + S212;
		S213t = S213t + S213;
		S222t = S222t + S222;
		S223t = S223t + S223;
		S232t = S232t + S232;
		S233t = S233t + S233;
		
		S311t = S311t + S213;
		S312t = S312t + S223;
		S313t = S313t + pp.x;
		S322t = S322t + S233;
		S323t = S323t + pp.y;
		S333t = npfinale;
	}

	double[][] S1lin = {{S111t, S112t, S113t},{S112t, S113t, S123t},{S113t, S123t, S133t}};
	Matrix S1 = new Matrix(S1lin);
	
	double[][] S2lin = {{S211t, S212t, S213t},{S212t, S222t, S223t},{S222t, S232t, S233t}};
	Matrix S2 = new Matrix(S2lin);	
	
	double[][] S3lin = {{S311t, S312t, S313t},{S312t, S322t, S323t},{S313t, S323t, S333t}};
	Matrix S3 = new Matrix(S3lin);

	double s3det = S3.det();
		if (s3det == 0)  {
		IJ.error("ERROR: S3 Matrix is not invertible");
		return;
		}
		
	
	double[][] C1invlin = {{0, 0, 0.5},{0, -1, 0},{0.5, 0, 0}};
	Matrix C1inv = new Matrix(C1invlin);

	Matrix S2t = S2.transpose();
	Matrix S3i = S3.inverse();
	
	Matrix S4 = S3i.times(S2t);
	Matrix S5 = S2.times(S4);
	Matrix S6 = S1.minus(S5);
	Matrix M = C1inv.times(S6);
	
	EigenvalueDecomposition Emme = M.eig();
	Matrix Eival = Emme.getD();
	Matrix Eivec = Emme.getV();
	
	double alpha1 = Eival.get(0,0);
	double alpha2 = Eival.get(1,1);
	double alpha3 = Eival.get(2,2);
	
	int evpos =0;
	for (int nr=0; nr<= 2; nr++) {
		if (Eival.get(nr,nr) > 0)  {
		evpos = nr;
		}		
	}
	Matrix abc = Eivec.getMatrix(0,2,evpos,evpos);
	
	Matrix def = S4.times(abc).times(-1);
	double acoef = abc.get(0,0);
	double bcoef = abc.get(1,0);
	double ccoef = abc.get(2,0);
	double dcoef = def.get(0,0);
	double ecoef = def.get(1,0);
	double fcoef = def.get(2,0);
	
	double verifica = 4*acoef*ccoef -bcoef*bcoef;
//	IJ.write("Autovalori: alpha1= "+ alpha1 + " ,alpha2= " + alpha2 + " ,alpha3=" + alpha3 + " verifica " + verifica);
//	IJ.write("Coefficienti: a= "+ acoef + " ,b= " + bcoef + " ,c=" + ccoef);

//	Matrice dei coefficienti	
	double[][] abcdeflin = {{acoef, bcoef/2, dcoef/2},{bcoef/2, ccoef, ecoef/2},{dcoef/2, ecoef/2, fcoef}};
	Matrix abcdef = new Matrix(abcdeflin);
	double abcdefdet = abcdef.det();
	double[][] a33lin = {{acoef, bcoef/2},{bcoef/2, ccoef}};
	Matrix a33 = new Matrix(a33lin);
	double a33det = a33.det();	
//	Calcolo centro ellisse	
	double[][] Sa2lin = {{-dcoef/2},{-ecoef/2}};
	Matrix Sa2 = new Matrix(Sa2lin);
	Matrix Xcentre = a33.solve(Sa2);
	double centrex= Xcentre.get(0,0);
	double centrey= Xcentre.get(1,0);
	
//	Calcolo i semi assi
	EigenvalueDecomposition Ava33 = a33.eig();
	Matrix Eivala33 = Ava33.getD();
	double beta1 = Eivala33.get(0,0);
	double beta2 = Eivala33.get(1,1);
	double semiasse2 = Math.sqrt((Math.abs(abcdefdet))/((Math.abs(beta1)*a33det)));
	double semiasse1 = Math.sqrt((Math.abs(abcdefdet))/((Math.abs(beta2)*a33det)));
	double eccentr = semiasse1/semiasse2;
	if (eccentr > 1) eccentr=1/eccentr;

//      rette assi alfaA(x - centrex) - betaA(y - centrey) = 0
//	e betaA(x - centrex) + alfaA(y - centrey) = 0
	double alfaA = bcoef;
	double betaA = acoef - ccoef + Math.sqrt(Math.pow((acoef-ccoef),2) + Math.pow(bcoef,2));
	double masse1 = alfaA/betaA;
	double qasse1 = ((betaA*centrey)-(alfaA*centrex))/betaA;
	double masse2 = -betaA/alfaA;
	double qasse2 = ((betaA*centrex)+(alfaA*centrey))/alfaA;
	double thetaas1 = (Math.atan(masse1));
	
// 	Punti intersezione con retta di base
	double c1 = acoef + (bcoef*m) + ccoef*Math.pow(m,2);
	double c2 = (bcoef*q) + (2*ccoef*m*q) + dcoef + (ecoef*m);
	double c3 = ccoef*Math.pow(q,2) + (ecoef*q) + fcoef;
	double delta = Math.pow(c2,2) - (4*c1*c3);
		if (delta < 0) {
		IJ.showMessage("The DROP doesn't touch the selected base line");
		return;
		}
		if (delta == 0) {
		IJ.showMessage("The Contact ANGLE of this DROP on the base line is 180 degrees");
		return;
		}		
		
	double xb1 = ((-c2+Math.sqrt(delta))/(2*c1));
	double yb1 = (m*xb1) +q;
	double xb2 = ((-c2-Math.sqrt(delta))/(2*c1));
	double yb2 = (m*xb2) +q;
//      Impongo che xb2 sia left, xb1 right
        double xbtest, ybtest;
        if (xb1 < xb2) {
		xbtest = xb1;
                ybtest = yb1;
                xb1 = xb2;
                yb1 = yb2;
                xb2 = xbtest;
                yb2 = ybtest;
	}
        
	int xb1i = (int)xb1;
	int yb1i = (int)yb1;
	int xb2i = (int)xb2;
	int yb2i = (int)yb2;
	//IJ.run("Revert");
	IJ.run("Line Width...", "line=2");
	IJ.makeLine(xb1i, yb1i, xb2i, yb2i);
	IJ.run("Draw","slice");
//	double wt = Math.sqrt((xb2-xb1)*(xb2-xb1) + (yb2-yb1)*(yb2-yb1));
	
//	Calcolo tangenze nei punti di intersezione y = mtg x + qtg ; 
	double[][] point1lin = {{xb1, yb1, 1}};
	Matrix point1 = new Matrix(point1lin);	
	double[][] point2lin = {{xb2, yb2, 1}};
	Matrix point2 = new Matrix(point2lin);
	Matrix tangc1 = point1.times(abcdef);
	Matrix tangc2 = point2.times(abcdef);
	double mtg1 = -tangc1.get(0,0)/tangc1.get(0,1);
	double qtg1 = -tangc1.get(0,2)/tangc1.get(0,1);
	double mtg2 = -tangc2.get(0,0)/tangc2.get(0,1);
	double qtg2 = -tangc2.get(0,2)/tangc2.get(0,1);
//      calcolo punti sulla cornice
	int xb3i =0;
        int yb3i =0;
        int xb4i =width;
        int yb4i =0;
        int xb5i =0;
        int yb5i =0;
        int xb6i =width;
        int yb6i =0;
        yb5i = (int)(qtg2);
        if (yb5i < 0) {
		yb5i = 0;
                xb5i = (int)(-qtg2/mtg2);
	}
        yb6i = (int)((mtg2*xb6i)+ qtg2);
        if (yb6i < 0) {
		yb6i = 0;
                xb6i = (int)(-qtg2/mtg2);
	}
        yb3i = (int)qtg1;
        if (yb3i < 0) {
		yb3i = 0;
                xb3i = (int)(-qtg1/mtg1);
	}
	yb4i = (int)((mtg1*xb4i)+ qtg1);
        if (yb4i < 0) {
		yb4i = 0;
                xb4i = (int)(-qtg1/mtg1);
	}
     	IJ.makeLine(xb3i, yb3i, xb4i, yb4i);
	IJ.run("Draw","slice");
	IJ.makeLine(xb5i, yb5i, xb6i, yb6i);
	IJ.run("Draw","slice");
//	IJ.write("coefficienti tg1 =" + mtg1 + " " + qtg1);
//	IJ.write("coefficienti tg2 =" + mtg2 + " " + qtg2);
        double thetal = Math.rint((Math.atan(mtg2)*1800/Math.PI))/10;
        double thetar = Math.rint((Math.atan(mtg1)*1800/Math.PI))/10;
	double thetapend = (Math.rint(Math.atan(m)*1800/Math.PI)/10);
        double thetargradi = thetar;
        double thetalgradi = thetal;
        if (thetal < 0) {
                thetalgradi = 180 + thetal;
                }

        if (thetar < 0) {
                thetargradi = -thetar;
                }
        else {
                thetargradi = 180-thetar;
                }                
		thetalgradi = thetalgradi - thetapend;
                thetargradi = thetargradi + thetapend;
                
                double thetaaverage = (thetalgradi + thetargradi)/2;

//	Calcolo errore fit ellisse GEOMETRICAL ST DEV
	double scrte =0;
	double scrtie =0;	
	for (int k=0; k< npfinale; k++) {
		Point pp = (Point) dropred.elementAt(k);
		scrte = scrte + Math.abs(acoef*Math.pow(pp.x,2) + bcoef* pp.x * pp.y + ccoef * Math.pow(pp.y,2) + dcoef*pp.x + ecoef*pp.y + fcoef);
	}
	double stdeve = Math.sqrt(scrte/(npfinale-1));
	

/* Disegno Ovale*/
		int NUMPOLY = 300;
		int[] xPoints, yPoints;
		xPoints = new int[NUMPOLY];
		yPoints = new int[NUMPOLY];
		double hTheta = 2*Math.PI/NUMPOLY;
		double cosasse1 = Math.cos(thetaas1);
		double sinasse1 = Math.sin(thetaas1);
		for (int i = 0; i < NUMPOLY; i++){
			double Rtheta = i*hTheta;
			double cos = Math.cos(Rtheta);
			double sin = Math.sin(Rtheta);
			double xor = semiasse1*cos;
			double yor = semiasse2*sin;
			xPoints[i] = (int)Math.round(centrex + (xor*cosasse1) - (yor*sinasse1));
			yPoints[i] = (int)Math.round(centrey + (xor*sinasse1) + (yor*cosasse1));
		}
		//PolygonRoi proi = new PolygonRoi(xPoints, yPoints,NUMPOLY,Roi.POLYGON);
		PolygonRoi proi = new PolygonRoi(xPoints, yPoints,NUMPOLY,Roi.TRACED_ROI);
		//proi.fitSpline(NUMSPLINE);
		imp.setRoi(proi);
		IJ.run("Draw","slice");
//Intersezione con l'asse maggiore ed asse minore
	 	c1 = acoef + (bcoef*masse1) + ccoef*Math.pow(masse1,2);
		c2 = (bcoef*qasse1) + (2*ccoef*masse1*qasse1) + dcoef + (ecoef*masse1);
		c3 = ccoef*Math.pow(qasse1,2) + (ecoef*qasse1) + fcoef;
		delta = Math.pow(c2,2) - (4*c1*c3);
		
		xb1 = ((-c2+Math.sqrt(delta))/(2*c1));
		yb1 = (masse1*xb1) +qasse1;
		xb2 = ((-c2-Math.sqrt(delta))/(2*c1));
		yb2 = (masse1*xb2) +qasse1;
			xb1i = (int)xb1;
			yb1i = (int)yb1;
			xb2i = (int)xb2;
			yb2i = (int)yb2;
		if (yb1i < 0) {
			yb1i = 0;
			xb1i = (int)(-qasse1/masse1);
		}
		if (yb2i < 0) {
			yb2i = 0;
			xb2i = (int)(-qasse1/masse1);
		}
	IJ.makeLine(xb1i, yb1i, xb2i, yb2i);
	IJ.run("Draw","slice");
	
	 	c1 = acoef + (bcoef*masse2) + ccoef*Math.pow(masse2,2);
		c2 = (bcoef*qasse2) + (2*ccoef*masse2*qasse2) + dcoef + (ecoef*masse2);
		c3 = ccoef*Math.pow(qasse2,2) + (ecoef*qasse2) + fcoef;
		delta = Math.pow(c2,2) - (4*c1*c3);
		
		xb1 = ((-c2+Math.sqrt(delta))/(2*c1));
		yb1 = (masse2*xb1) +qasse2;
		xb2 = ((-c2-Math.sqrt(delta))/(2*c1));
		yb2 = (masse2*xb2) +qasse2;
			xb1i = (int)xb1;
			yb1i = (int)yb1;
			xb2i = (int)xb2;
			yb2i = (int)yb2;
		if (yb1i < 0) {
			yb1i = 0;
			xb1i = (int)(-qasse2/masse2);
		}
		if (yb2i < 0) {
			yb2i = 0;
			xb2i = (int)(-qasse2/masse2);
		}
	IJ.makeLine(xb1i, yb1i, xb2i, yb2i);
	IJ.run("Draw","slice");

//inizio curcle best fit: a,b centro supposto
       final Vector fi = new Vector(0, 16);
	int a0= (int)Xc.get(0,0);
	int b0= (int)Xc.get(1,0);
	int ai = a0;
	int bi = b0;
        double naf = 0;
	double fijt = 0;
// nr numero massimo di iterazioni fatte: normalmente converge in meno di 10 
	for (int nr=0; nr<= 99; nr++) {
		double fijxt = 0;
		double fijyt = 0;
		double laa1t = 0;
		double laa2t = 0;
		double lab1t = 0;
		double lab2t = 0;
		double lbb2t = 0;
		fijt = 0;		
		for (int k=0; k< npfinale; k++) {
			Point pp = (Point) dropred.elementAt(k);
			double fijx =(ai- pp.x);
			double fijy =(bi- pp.y);
			double fij = Math.sqrt(Math.pow(fijx,2) + Math.pow(fijy,2));

		if (fij == 0)  {
		IJ.error("ERROR: Fij Parameter equal to ZERO");
		return;
		}

			double laa1 = fijx/fij;
			double laa2 = Math.pow(fijy,2)/Math.pow(fij,3);
			double lab1 = fijy/fij;
			double lab2 = laa1*lab1/fij;
			double lbb2 = Math.pow(fijx,2)/Math.pow(fij,3);
			fijxt = fijxt + fijx;
			fijyt = fijyt + fijy;
			fijt = fijt + fij;
			laa1t = laa1t + laa1;
			laa2t = laa2t + laa2;
			lab1t = lab1t + lab1;
			lab2t = lab2t + lab2;
			lbb2t = lbb2t + lbb2;
		}		
		
		double laa = npfinale - (1/npfinale)*Math.pow(laa1t,2) - (1/npfinale)*fijt*laa2t;
		double lab = (-1/npfinale)*lab1t*laa1t + (1/npfinale)*fijt*lab2t;
		double lbb = npfinale - (1/npfinale)*Math.pow(lab1t,2) - (1/npfinale)*fijt*lbb2t;
		double f1 = fijxt - (1/npfinale)*fijt*laa1t;
		double f2 = fijyt - (1/npfinale)*fijt*lab1t;
		double fract = (Math.pow(lab,2) - laa*lbb);

		if (fract == 0)  {
		IJ.error("ERROR: fract Parameter equal to ZERO");
		return;
		}

		double deltaai = (lbb*f1 - lab*f2)/fract;
		double deltabi = (laa*f2 - lab*f1)/fract;
		int deltaa = (int)deltaai;
		int deltab = (int)deltabi;
		ai = ai + deltaa;
		bi = bi + deltab;
		if (deltaa==0 && deltab==0) {
                        naf = nr;
			nr=1000;
			}
		//if spostamento centro < 1 esci
	}
		double rag = ((1/npfinale)*fijt);
		
/* calcolo GEOMETRICAL STANDARD DEVIATION of the best fit function */
		double scrt =0;
		double scrti =0;
		for (int k=0; k< npfinale; k++) {
			Point pp = (Point) dropred.elementAt(k);
			double fijx =(ai- pp.x);
			double fijy =(bi- pp.y);
		scrti = Math.pow(Math.sqrt(Math.pow(fijx,2) + Math.pow(fijy,2)) - rag,2);
		scrt = scrt + scrti;
		}
		double stdev = Math.sqrt(scrt/(npfinale-1));
// calcolo punti intersezione circonferenza e retta di base
		double b1 = 2*(q*m - ai - m*bi)/(1+m*m);
		double b2 = (ai*ai +q*q + bi*bi -2*bi*q - rag*rag)/(1+m*m);
		delta = b1*b1 - 4*b2;
		if (delta < 0) {
			IJ.showMessage("CIRCLE: The DROP doesn't touch the selected base line");
			return;
			}
		if (delta == 0) {
			IJ.showMessage("CIRCLE: The Contact ANGLE of this DROP on the base line is 180 degrees");
			return;
			}		
		xb1 = ((-b1+Math.sqrt(delta))/2);
		yb1 = (m*xb1 +q);
		xb2 = ((-b1-Math.sqrt(delta))/2);
		yb2 = (m*xb2 +q);
		xb1i = (int)xb1;
		yb1i = (int)yb1;
		xb2i = (int)xb2;
		yb2i = (int)yb2;

		IJ.makeLine(xb1i, yb1i, xb2i, yb2i);
		IJ.run("Draw","slice");
		double wt = Math.sqrt((xb2-xb1)*(xb2-xb1) + (yb2-yb1)*(yb2-yb1));

/* calcolo culmine: distanza centro - base + raggio punto centrale*/
		double cbt =  -(m*ai - bi + q)/Math.sqrt(m*m + 1);
		double ht = rag + cbt;
		double exsol=2*ht/wt;
		double thetac = 2*Math.atan(exsol);
		double area = 2*Math.PI*rag*ht;
		double volume = ht*ht*(3*rag - ht)*Math.PI/3;
/* Error propagation */
		double error = 4*Math.sqrt(2)*stdev/((1+exsol)*wt);
		
//PRINT RESULTS
		IJ.run("Set Measurements...", "display decimal=14");
		
		Roi roi = imp.getRoi();
		Analyzer a = new Analyzer();
		ImageStatistics stats = imp.getStatistics(Analyzer.getMeasurements());
		a.saveResults(stats, roi); // store in system results table
		ResultsTable rt =Analyzer.getResultsTable(); // get the system results table
		rt.addLabel("File Name", imgFileName);	
		rt.addValue("Frame", imp.getCurrentSlice());
		rt.addValue("Theta C", (Math.rint(thetac*1800/Math.PI)/10));
		rt.addValue("Uncertainty", (Math.rint(error*1800/Math.PI)/10));
		
		rt.addValue("Theta Left", ((Math.rint(thetalgradi*10))/10));
		rt.addValue("Theta Right", ((Math.rint(thetargradi*10))/10));
		rt.addValue("Theta E", (Math.rint(thetaaverage*10))/10);
		
		rt.addValue("Circle StDev", stdev);
		rt.addValue("Ellipse StDev", stdeve);
		rt.addValue("e", Math.rint(eccentr*100)/100);
/*		rt.addValue("Radius", Math.rint(rag*100)/100);
                rt.addValue("X Centre", ai);
                rt.addValue("Y Centre", bi);
                rt.addValue("Iterations:", naf); */
                rt.addValue("Points:", npfinale);
		rt.addValue("Volume", Math.rint(volume*100)/100);
		rt.addValue("Area Sup", Math.rint(area*100)/100);
		rt.addValue("Base", Math.rint(wt*100)/100);
		rt.addValue("Height", Math.rint(ht*100)/100);
		a.displayResults();
		a.updateHeadings();
		

		double ovalxb = (ai - rag);
		double ovalyb = (bi - rag);
		IJ.run("Line Width...", "line=3");

		IJ.makeOval((int)(ovalxb), (int)(ovalyb), (int)(2*rag), (int)(2*rag));
		IJ.run("Draw","slice");
	
		}


		
/*********************************************************************
 *Inizio procedura calcolo con approssimazione sferica 5+ punti: 2 for BASE SELECTION
 ********************************************************************/
 	else if (ae.getActionCommand().equals("Manual Points Procedure")) {
		final Vector dropred = ph.getPoints();
		if (dropred.size() < 5)  {
		IJ.error("choose AT LEAST 5 points: the first 2 for the base line, the other 3 for the drop profile");
		return;
		}
		Point p1 = (Point) dropred.elementAt(0);
		Point p2 = (Point) dropred.elementAt(1);
		Point p3b = (Point) dropred.elementAt(2);
		Point p4 = (Point) dropred.elementAt(3);
		Point p5 = (Point) dropred.elementAt(4);

// calcolo retta di base tra p1 e p2: m X + q = y
		double dify = p1.y - p2.y;
		double difx = p1.x - p2.x;
		double m = dify/difx;
		double q = p1.y- m*p1.x;
// circonferenza tra p3, p4 e p5
		double[][] Acvalues = {{2*p3b.x-2*p4.x, 2*p3b.y-2*p4.y},{2*p3b.x-2*p5.x, 2*p3b.y-2*p5.y}}; 
		double[][] Scvalues = {{p3b.x*p3b.x+p3b.y*p3b.y-p4.x*p4.x-p4.y*p4.y},{p3b.x*p3b.x+p3b.y*p3b.y-p5.x*p5.x-p5.y*p5.y}};
		Matrix Ac = new Matrix(Acvalues);
		Matrix Sc = new Matrix(Scvalues);
		Matrix Xc = Ac.solve(Sc);
		double ragc = Math.sqrt(p4.x * p4.x + p4.y * p4.y - 2* p4.x * Xc.get(0,0) - 2* p4.y * Xc.get(1,0) + Xc.get(0,0)*Xc.get(0,0) + Xc.get(1,0)*Xc.get(1,0));

		// aggiungo n copie di ciascuno dei punti base p1 e p2
if (this.checkP1) {	
	for (int k=0; k<this.numP1; k++) {
		Point pnew1=new Point(p1);
		dropred.addElement(pnew1);
	}
}
if (this.checkP2) {
	for (int k=0; k<this.numP2; k++) {			
		Point pnew2=new Point(p2);
		dropred.addElement(pnew2);
	}
}
		
//SOLO 5 punti
	if (dropred.size() == 5)  {

/* calcolo punti intersezione circonferenza e retta di base   */
		String imgFileName = imp.getTitle();
		double b1 = 2*(q*m - Xc.get(0,0) - m*Xc.get(1,0))/(1+m*m);
		double b2 = (Xc.get(0,0)*Xc.get(0,0) +q*q + Xc.get(1,0)*Xc.get(1,0) -2*Xc.get(1,0)*q - ragc*ragc)/(1+m*m);
		double delta = b1*b1 - 4*b2;
		if (delta < 0) {
			IJ.showMessage("The DROP doesn't touch the selected base line");
			return;
			}
		if (delta == 0) {
			IJ.showMessage("The Contact ANGLE of this DROP on the base line is 180 degrees");
			return;
			}		
		double xb1 = ((-b1+Math.sqrt(delta))/2);
		double yb1 = (m*xb1 +q);
		double xb2 = ((-b1-Math.sqrt(delta))/2);
		double yb2 = (m*xb2 +q);
		int xb1i = (int)xb1;
		int yb1i = (int)yb1;
		int xb2i = (int)xb2;
		int yb2i = (int)yb2;

		IJ.run("Line Width...", "line=3");
		IJ.makeLine(xb1i, yb1i, xb2i, yb2i);
		IJ.run("Draw","slice");
		double wt = Math.sqrt((xb2-xb1)*(xb2-xb1) + (yb2-yb1)*(yb2-yb1));

/* calcolo culmine: distanza centro - base + raggio punto centrale*/
		double cbt =  -(m*Xc.get(0,0) - Xc.get(1,0) + q)/Math.sqrt(m*m + 1);
		double ht = ragc + cbt;
		double thetac = 2*Math.atan(2*ht/wt);
		double area = 2*Math.PI*ragc*ht;
		double volume = ht*ht*(3*ragc - ht)*Math.PI/3;

		IJ.run("Set Measurements...", "display decimal=14");
		
		Roi roi = imp.getRoi();
		Analyzer a = new Analyzer();
		ImageStatistics stats = imp.getStatistics(Analyzer.getMeasurements());
		a.saveResults(stats, roi); // store in system results table
		ResultsTable rt =Analyzer.getResultsTable(); // get the system results table
		rt.addLabel("File Name", imgFileName);	
		rt.addValue("Frame", imp.getCurrentSlice());	
		rt.addValue("Theta", (Math.rint(thetac*1800/Math.PI)/10));
		rt.addValue("Radius", Math.rint(ragc*100)/100);
                rt.addValue("X Centre:", Math.rint(Xc.get(0,0)));
                rt.addValue("Y Centro:", Math.rint(Xc.get(1,0)));
		rt.addValue("Volume", Math.rint(volume*100)/100);
		rt.addValue("Area Sup", Math.rint(area*100)/100);
		rt.addValue("Height", Math.rint(ht*100)/100);
		a.displayResults();
		a.updateHeadings();
		

		double ovalxb = (Xc.get(0,0) - ragc);
		double ovalyb = (Xc.get(1,0) - ragc);
		IJ.run("Line Width...", "line=3");
		IJ.makeOval((int)(ovalxb), (int)(ovalyb), (int)(2*ragc), (int)(2*ragc));
		IJ.run("Draw","slice");
	}
else {
	
ImageProcessor ip = imp.getProcessor();
String imgFileName = imp.getTitle();
int width = imp.getWidth();
int height = imp.getHeight();
dropred.removeElementAt(1);
dropred.removeElementAt(0);
double npfinale = dropred.size();

double delta, xb1, yb1, xb2, yb2,thetalgradi=0, thetargradi=0, thetaaverage=0, stdeve=0, eccentr =0;
int xb1i, yb1i, xb2i, yb2i;
if (dropred.size() >= 5)  {
			
//inizio best fit ellisse: calcolo delle matrici s1, s2 ed s3

	double S111t = 0;
	double S112t = 0;
	double S113t = 0;
	double S123t = 0;
	double S133t = 0;

	double S211t = 0;
	double S212t = 0;
	double S213t = 0;
	double S222t = 0;
	double S223t = 0;
	double S232t = 0;
	double S233t = 0;

	double S311t = 0;
	double S312t = 0;
	double S313t = 0;
	double S322t = 0;
	double S323t = 0;
	double S333t = 0;	

	for (int k=0; k< npfinale; k++) {
		Point pp = (Point) dropred.elementAt(k);
		double S111 = Math.pow(pp.x,4);
		double S112 = Math.pow(pp.x,3) * pp.y;
		double S113 = Math.pow(pp.x,2) * Math.pow(pp.y,2);
		double S123 = Math.pow(pp.x,1) * Math.pow(pp.y,3);
		double S133 = Math.pow(pp.y,4);
		
		double S211 = Math.pow(pp.x,3);
		double S212 = Math.pow(pp.x,2) * Math.pow(pp.y,1);
		double S213 = Math.pow(pp.x,2);
		double S222 = Math.pow(pp.x,1) * Math.pow(pp.y,2);
		double S223 = Math.pow(pp.x,1) * Math.pow(pp.y,1);
		double S232 = Math.pow(pp.y,3);
		double S233 = Math.pow(pp.y,2);
		
		S111t = S111t + S111;
		S112t = S112t + S112;
		S113t = S113t + S113;
		S123t = S123t + S123;
		S133t = S133t + S133;
		
		S211t = S211t + S211;
		S212t = S212t + S212;
		S213t = S213t + S213;
		S222t = S222t + S222;
		S223t = S223t + S223;
		S232t = S232t + S232;
		S233t = S233t + S233;
		
		S311t = S311t + S213;
		S312t = S312t + S223;
		S313t = S313t + pp.x;
		S322t = S322t + S233;
		S323t = S323t + pp.y;
		S333t = npfinale;
	}

	double[][] S1lin = {{S111t, S112t, S113t},{S112t, S113t, S123t},{S113t, S123t, S133t}};
	Matrix S1 = new Matrix(S1lin);
	
	double[][] S2lin = {{S211t, S212t, S213t},{S212t, S222t, S223t},{S222t, S232t, S233t}};
	Matrix S2 = new Matrix(S2lin);	
	
	double[][] S3lin = {{S311t, S312t, S313t},{S312t, S322t, S323t},{S313t, S323t, S333t}};
	Matrix S3 = new Matrix(S3lin);

	double s3det = S3.det();
		if (s3det == 0)  {
		IJ.error("ERROR: S3 Matrix is not invertible");
		return;
		}
		
	
	double[][] C1invlin = {{0, 0, 0.5},{0, -1, 0},{0.5, 0, 0}};
	Matrix C1inv = new Matrix(C1invlin);

	Matrix S2t = S2.transpose();
	Matrix S3i = S3.inverse();
	
	Matrix S4 = S3i.times(S2t);
	Matrix S5 = S2.times(S4);
	Matrix S6 = S1.minus(S5);
	Matrix M = C1inv.times(S6);
	
	EigenvalueDecomposition Emme = M.eig();
	Matrix Eival = Emme.getD();
	Matrix Eivec = Emme.getV();
	
	double alpha1 = Eival.get(0,0);
	double alpha2 = Eival.get(1,1);
	double alpha3 = Eival.get(2,2);
	
	int evpos =0;
	for (int nr=0; nr<= 2; nr++) {
		if (Eival.get(nr,nr) > 0)  {
		evpos = nr;
		}		
	}
	Matrix abc = Eivec.getMatrix(0,2,evpos,evpos);
	
	Matrix def = S4.times(abc).times(-1);
	double acoef = abc.get(0,0);
	double bcoef = abc.get(1,0);
	double ccoef = abc.get(2,0);
	double dcoef = def.get(0,0);
	double ecoef = def.get(1,0);
	double fcoef = def.get(2,0);
	
	double verifica = 4*acoef*ccoef -bcoef*bcoef;

//	Matrice dei coefficienti	
	double[][] abcdeflin = {{acoef, bcoef/2, dcoef/2},{bcoef/2, ccoef, ecoef/2},{dcoef/2, ecoef/2, fcoef}};
	Matrix abcdef = new Matrix(abcdeflin);
	double abcdefdet = abcdef.det();
	double[][] a33lin = {{acoef, bcoef/2},{bcoef/2, ccoef}};
	Matrix a33 = new Matrix(a33lin);
	double a33det = a33.det();	
//	Calcolo centro ellisse	
	double[][] Sa2lin = {{-dcoef/2},{-ecoef/2}};
	Matrix Sa2 = new Matrix(Sa2lin);
	Matrix Xcentre = a33.solve(Sa2);
	double centrex= Xcentre.get(0,0);
	double centrey= Xcentre.get(1,0);
	
//	Calcolo i semi assi
	EigenvalueDecomposition Ava33 = a33.eig();
	Matrix Eivala33 = Ava33.getD();
	double beta1 = Eivala33.get(0,0);
	double beta2 = Eivala33.get(1,1);
	double semiasse2 = Math.sqrt((Math.abs(abcdefdet))/((Math.abs(beta1)*a33det)));
	double semiasse1 = Math.sqrt((Math.abs(abcdefdet))/((Math.abs(beta2)*a33det)));
	eccentr = semiasse1/semiasse2;
	if (eccentr > 1) eccentr=1/eccentr;

//      rette assi alfaA(x - centrex) - betaA(y - centrey) = 0
//	e betaA(x - centrex) + alfaA(y - centrey) = 0
	double alfaA = bcoef;
	double betaA = acoef - ccoef + Math.sqrt(Math.pow((acoef-ccoef),2) + Math.pow(bcoef,2));
	double masse1 = alfaA/betaA;
	double qasse1 = ((betaA*centrey)-(alfaA*centrex))/betaA;
	double masse2 = -betaA/alfaA;
	double qasse2 = ((betaA*centrex)+(alfaA*centrey))/alfaA;
	double thetaas1 = (Math.atan(masse1));
	
// 	Punti intersezione con retta di base
	double c1 = acoef + (bcoef*m) + ccoef*Math.pow(m,2);
	double c2 = (bcoef*q) + (2*ccoef*m*q) + dcoef + (ecoef*m);
	double c3 = ccoef*Math.pow(q,2) + (ecoef*q) + fcoef;
	delta = Math.pow(c2,2) - (4*c1*c3);
		if (delta < 0) {
		IJ.showMessage("The DROP doesn't touch the selected base line");
		return;
		}
		if (delta == 0) {
		IJ.showMessage("The Contact ANGLE of this DROP on the base line is 180 degrees");
		return;
		}		
		
	xb1 = ((-c2+Math.sqrt(delta))/(2*c1));
	yb1 = (m*xb1) +q;
	xb2 = ((-c2-Math.sqrt(delta))/(2*c1));
	yb2 = (m*xb2) +q;
//      Impongo che xb2 sia left, xb1 right
        double xbtest, ybtest;
        if (xb1 < xb2) {
		xbtest = xb1;
                ybtest = yb1;
                xb1 = xb2;
                yb1 = yb2;
                xb2 = xbtest;
                yb2 = ybtest;
	}
        
	xb1i = (int)xb1;
	yb1i = (int)yb1;
	xb2i = (int)xb2;
	yb2i = (int)yb2;
//	IJ.run("Revert");
	IJ.run("Line Width...", "line=2");
	IJ.makeLine(xb1i, yb1i, xb2i, yb2i);
	IJ.run("Draw","slice");
//	double wt = Math.sqrt((xb2-xb1)*(xb2-xb1) + (yb2-yb1)*(yb2-yb1));
	
//	Calcolo tangenze nei punti di intersezione y = mtg x + qtg ; 
	double[][] point1lin = {{xb1, yb1, 1}};
	Matrix point1 = new Matrix(point1lin);	
	double[][] point2lin = {{xb2, yb2, 1}};
	Matrix point2 = new Matrix(point2lin);
	Matrix tangc1 = point1.times(abcdef);
	Matrix tangc2 = point2.times(abcdef);
	double mtg1 = -tangc1.get(0,0)/tangc1.get(0,1);
	double qtg1 = -tangc1.get(0,2)/tangc1.get(0,1);
	double mtg2 = -tangc2.get(0,0)/tangc2.get(0,1);
	double qtg2 = -tangc2.get(0,2)/tangc2.get(0,1);
//      calcolo punti sulla cornice
	int xb3i =0;
        int yb3i =0;
        int xb4i =width;
        int yb4i =0;
        int xb5i =0;
        int yb5i =0;
        int xb6i =width;
        int yb6i =0;
        yb5i = (int)(qtg2);
        if (yb5i < 0) {
		yb5i = 0;
                xb5i = (int)(-qtg2/mtg2);
	}
        yb6i = (int)((mtg2*xb6i)+ qtg2);
        if (yb6i < 0) {
		yb6i = 0;
                xb6i = (int)(-qtg2/mtg2);
	}
        yb3i = (int)qtg1;
        if (yb3i < 0) {
		yb3i = 0;
                xb3i = (int)(-qtg1/mtg1);
	}
	yb4i = (int)((mtg1*xb4i)+ qtg1);
        if (yb4i < 0) {
		yb4i = 0;
                xb4i = (int)(-qtg1/mtg1);
	}
     	IJ.makeLine(xb3i, yb3i, xb4i, yb4i);
	IJ.run("Draw","slice");
	IJ.makeLine(xb5i, yb5i, xb6i, yb6i);
	IJ.run("Draw","slice");
//	IJ.write("coefficienti tg1 =" + mtg1 + " " + qtg1);
//	IJ.write("coefficienti tg2 =" + mtg2 + " " + qtg2);
        double thetal = Math.rint((Math.atan(mtg2)*1800/Math.PI))/10;
        double thetar = Math.rint((Math.atan(mtg1)*1800/Math.PI))/10;
	double thetapend = (Math.rint(Math.atan(m)*1800/Math.PI)/10);
        thetargradi = thetar;
        thetalgradi = thetal;
        if (thetal < 0) {
                thetalgradi = 180 + thetal;
                }

        if (thetar < 0) {
                thetargradi = -thetar;
                }
        else {
                thetargradi = 180-thetar;
                }                
	thetalgradi = thetalgradi - thetapend;
        thetargradi = thetargradi + thetapend;
        thetaaverage = (thetalgradi + thetargradi)/2;

//	Calcolo errore fit ellisse GEOMETRICAL ST DEV
	double scrte =0;
	double scrtie =0;	
	for (int k=0; k< npfinale; k++) {
		Point pp = (Point) dropred.elementAt(k);
		scrte = scrte + Math.abs(acoef*Math.pow(pp.x,2) + bcoef* pp.x * pp.y + ccoef * Math.pow(pp.y,2) + dcoef*pp.x + ecoef*pp.y + fcoef);
	}
	stdeve = Math.sqrt(scrte/(npfinale-1));
	

/* Disegno Ovale*/
		int NUMPOLY = 300;
		int[] xPoints, yPoints;
		xPoints = new int[NUMPOLY];
		yPoints = new int[NUMPOLY];
		double hTheta = 2*Math.PI/NUMPOLY;
		double cosasse1 = Math.cos(thetaas1);
		double sinasse1 = Math.sin(thetaas1);
		for (int i = 0; i < NUMPOLY; i++){
			double Rtheta = i*hTheta;
			double cos = Math.cos(Rtheta);
			double sin = Math.sin(Rtheta);
			double xor = semiasse1*cos;
			double yor = semiasse2*sin;
			xPoints[i] = (int)Math.round(centrex + (xor*cosasse1) - (yor*sinasse1));
			yPoints[i] = (int)Math.round(centrey + (xor*sinasse1) + (yor*cosasse1));
		}
		//PolygonRoi proi = new PolygonRoi(xPoints, yPoints,NUMPOLY,Roi.POLYGON);
		PolygonRoi proi = new PolygonRoi(xPoints, yPoints,NUMPOLY,Roi.TRACED_ROI);
		//proi.fitSpline(NUMSPLINE);
		imp.setRoi(proi);
		IJ.run("Draw","slice");
//Intersezione con l'asse maggiore ed asse minore
	 	c1 = acoef + (bcoef*masse1) + ccoef*Math.pow(masse1,2);
		c2 = (bcoef*qasse1) + (2*ccoef*masse1*qasse1) + dcoef + (ecoef*masse1);
		c3 = ccoef*Math.pow(qasse1,2) + (ecoef*qasse1) + fcoef;
		delta = Math.pow(c2,2) - (4*c1*c3);
		
		xb1 = ((-c2+Math.sqrt(delta))/(2*c1));
		yb1 = (masse1*xb1) +qasse1;
		xb2 = ((-c2-Math.sqrt(delta))/(2*c1));
		yb2 = (masse1*xb2) +qasse1;
			xb1i = (int)xb1;
			yb1i = (int)yb1;
			xb2i = (int)xb2;
			yb2i = (int)yb2;
		if (yb1i < 0) {
			yb1i = 0;
			xb1i = (int)(-qasse1/masse1);
		}
		if (yb2i < 0) {
			yb2i = 0;
			xb2i = (int)(-qasse1/masse1);
		}
	IJ.makeLine(xb1i, yb1i, xb2i, yb2i);
	IJ.run("Draw","slice");
	
	 	c1 = acoef + (bcoef*masse2) + ccoef*Math.pow(masse2,2);
		c2 = (bcoef*qasse2) + (2*ccoef*masse2*qasse2) + dcoef + (ecoef*masse2);
		c3 = ccoef*Math.pow(qasse2,2) + (ecoef*qasse2) + fcoef;
		delta = Math.pow(c2,2) - (4*c1*c3);
		
		xb1 = ((-c2+Math.sqrt(delta))/(2*c1));
		yb1 = (masse2*xb1) +qasse2;
		xb2 = ((-c2-Math.sqrt(delta))/(2*c1));
		yb2 = (masse2*xb2) +qasse2;
			xb1i = (int)xb1;
			yb1i = (int)yb1;
			xb2i = (int)xb2;
			yb2i = (int)yb2;
		if (yb1i < 0) {
			yb1i = 0;
			xb1i = (int)(-qasse2/masse2);
		}
		if (yb2i < 0) {
			yb2i = 0;
			xb2i = (int)(-qasse2/masse2);
		}
	IJ.makeLine(xb1i, yb1i, xb2i, yb2i);
	IJ.run("Draw","slice");
}
//inizio curcle best fit: a,b centro supposto
       final Vector fi = new Vector(0, 16);
	int a0= (int)Xc.get(0,0);
	int b0= (int)Xc.get(1,0);
	int ai = a0;
	int bi = b0;
        double naf = 0;
	double fijt = 0;
// nr numero massimo di iterazioni fatte: normalmente converge in meno di 10 
	for (int nr=0; nr<= 99; nr++) {
		double fijxt = 0;
		double fijyt = 0;
		double laa1t = 0;
		double laa2t = 0;
		double lab1t = 0;
		double lab2t = 0;
		double lbb2t = 0;
		fijt = 0;		
		for (int k=0; k< npfinale; k++) {
			Point pp = (Point) dropred.elementAt(k);
			double fijx =(ai- pp.x);
			double fijy =(bi- pp.y);
			double fij = Math.sqrt(Math.pow(fijx,2) + Math.pow(fijy,2));

		if (fij == 0)  {
		IJ.error("ERROR: Fij Parameter equal to ZERO");
		return;
		}

			double laa1 = fijx/fij;
			double laa2 = Math.pow(fijy,2)/Math.pow(fij,3);
			double lab1 = fijy/fij;
			double lab2 = laa1*lab1/fij;
			double lbb2 = Math.pow(fijx,2)/Math.pow(fij,3);
			fijxt = fijxt + fijx;
			fijyt = fijyt + fijy;
			fijt = fijt + fij;
			laa1t = laa1t + laa1;
			laa2t = laa2t + laa2;
			lab1t = lab1t + lab1;
			lab2t = lab2t + lab2;
			lbb2t = lbb2t + lbb2;
		}		
		
		double laa = npfinale - (1/npfinale)*Math.pow(laa1t,2) - (1/npfinale)*fijt*laa2t;
		double lab = (-1/npfinale)*lab1t*laa1t + (1/npfinale)*fijt*lab2t;
		double lbb = npfinale - (1/npfinale)*Math.pow(lab1t,2) - (1/npfinale)*fijt*lbb2t;
		double f1 = fijxt - (1/npfinale)*fijt*laa1t;
		double f2 = fijyt - (1/npfinale)*fijt*lab1t;
		double fract = (Math.pow(lab,2) - laa*lbb);

		if (fract == 0)  {
		IJ.error("ERROR: fract Parameter equal to ZERO");
		return;
		}

		double deltaai = (lbb*f1 - lab*f2)/fract;
		double deltabi = (laa*f2 - lab*f1)/fract;
		int deltaa = (int)deltaai;
		int deltab = (int)deltabi;
		ai = ai + deltaa;
		bi = bi + deltab;
		if (deltaa==0 && deltab==0) {
                        naf = nr;
			nr=1000;
			}
		//if spostamento centro < 1 esci
	}
		double rag = ((1/npfinale)*fijt);
		
/* calcolo GEOMETRICAL STANDARD DEVIATION of the best fit function */
		double scrt =0;
		double scrti =0;
		for (int k=0; k< npfinale; k++) {
			Point pp = (Point) dropred.elementAt(k);
			double fijx =(ai- pp.x);
			double fijy =(bi- pp.y);
		scrti = Math.pow(Math.sqrt(Math.pow(fijx,2) + Math.pow(fijy,2)) - rag,2);
		scrt = scrt + scrti;
		}
		double stdev = Math.sqrt(scrt/(npfinale-1));
// calcolo punti intersezione circonferenza e retta di base
		double b1 = 2*(q*m - ai - m*bi)/(1+m*m);
		double b2 = (ai*ai +q*q + bi*bi -2*bi*q - rag*rag)/(1+m*m);
		delta = b1*b1 - 4*b2;
		if (delta < 0) {
			IJ.showMessage("CIRCLE: The DROP doesn't touch the selected base line");
			return;
			}
		if (delta == 0) {
			IJ.showMessage("CIRCLE: The Contact ANGLE of this DROP on the base line is 180 degrees");
			return;
			}		
		xb1 = ((-b1+Math.sqrt(delta))/2);
		yb1 = (m*xb1 +q);
		xb2 = ((-b1-Math.sqrt(delta))/2);
		yb2 = (m*xb2 +q);
		xb1i = (int)xb1;
		yb1i = (int)yb1;
		xb2i = (int)xb2;
		yb2i = (int)yb2;

		IJ.makeLine(xb1i, yb1i, xb2i, yb2i);
		IJ.run("Draw","slice");
		double wt = Math.sqrt((xb2-xb1)*(xb2-xb1) + (yb2-yb1)*(yb2-yb1));

/* calcolo culmine: distanza centro - base + raggio punto centrale*/
		double cbt =  -(m*ai - bi + q)/Math.sqrt(m*m + 1);
		double ht = rag + cbt;
		double exsol=2*ht/wt;
		double thetac = 2*Math.atan(exsol);
		double area = 2*Math.PI*rag*ht;
		double volume = ht*ht*(3*rag - ht)*Math.PI/3;
/* Error propagation */
		double error = 4*Math.sqrt(2)*stdev/((1+exsol)*wt);
		
//PRINT RESULTS
		IJ.run("Set Measurements...", "display decimal=14");
		
		Roi roi = imp.getRoi();
		Analyzer a = new Analyzer();
		ImageStatistics stats = imp.getStatistics(Analyzer.getMeasurements());
		a.saveResults(stats, roi); // store in system results table
		ResultsTable rt =Analyzer.getResultsTable(); // get the system results table
		rt.addLabel("File Name", imgFileName);	
		rt.addValue("Frame", imp.getCurrentSlice());
		rt.addValue("Theta C", (Math.rint(thetac*1800/Math.PI)/10));
		rt.addValue("Uncertainty", (Math.rint(error*1800/Math.PI)/10));
		
		rt.addValue("Theta Left", ((Math.rint(thetalgradi*10))/10));
		rt.addValue("Theta Right", ((Math.rint(thetargradi*10))/10));
		rt.addValue("Theta E", (Math.rint(thetaaverage*10))/10);
		rt.addValue("Radius", Math.rint(rag*100)/100);		
		rt.addValue("Circle StDev", stdev);
		rt.addValue("Ellipse StDev", stdeve);
		rt.addValue("e", Math.rint(eccentr*100)/100);

/*              rt.addValue("X Centre", ai);
                rt.addValue("Y Centre", bi);
                rt.addValue("Iterations:", naf); */
                rt.addValue("Points:", npfinale);
		rt.addValue("Volume", Math.rint(volume*100)/100);
		rt.addValue("Area Sup", Math.rint(area*100)/100);
		rt.addValue("Base", Math.rint(wt*100)/100);
		rt.addValue("Height", Math.rint(ht*100)/100);
		a.displayResults();
		a.updateHeadings();
		

		double ovalxb = (ai - rag);
		double ovalyb = (bi - rag);
		IJ.run("Line Width...", "line=3");

		IJ.makeOval((int)(ovalxb), (int)(ovalyb), (int)(2*rag), (int)(2*rag));
		IJ.run("Draw","slice");
		
	}
	}
	else if (ae.getActionCommand().equals("Cancel")) {
	}
} /* end actionPerformed */

/*********************************************************************
 * Return some additional margin to the dialog, for aesthetic purposes.
 * Necessary for the current MacOS X Java version, lest the first item
 * disappears from the frame.
 ********************************************************************/
public Insets getInsets (
) {
	return(new Insets(0, 20, 20, 20));
} /* end getInsets */

/*********************************************************************
 * This constructor stores a local copy of its parameters and prepares
 * the layout of the dialog.
 *
 * @param parentWindow Parent window.
 * @param ph <code>caHandler<code> object that handles operations.
 * @param imp <code>ImagePlus<code> object where points are being picked.
 ********************************************************************/
caFile (
	final Frame parentWindow,
	final caHandler ph,
	final ImagePlus imp
) {
	super(parentWindow, "Point List", true);
	this.ph = ph;
	this.imp = imp;
	setLayout(new GridLayout(0, 1));
	final Button saveAsButton = new Button("Save as");
	final Button showButton = new Button("Show");
	final Button openButton = new Button("Open");
	final Button calcola3Button = new Button("Circle BestFit");
	calcola3Button.addActionListener(this);
	final Button calcola5Button = new Button("Manual Points Procedure");
	calcola5Button.addActionListener(this);
	final Button ellipseButton = new Button("Ellipse Bestfit");
	ellipseButton.addActionListener(this);
	final Button circellButton = new Button("Both Bestfits");
	circellButton.addActionListener(this);
	final Button boundaryButton = new Button("Boundary Conditions");
	boundaryButton.addActionListener(this);
	final Button cancelButton = new Button("Cancel");
	saveAsButton.addActionListener(this);
	showButton.addActionListener(this);
	openButton.addActionListener(this);
	cancelButton.addActionListener(this);
	final Label separation1 = new Label("");
	final Label separation2 = new Label("");
	add(separation1);
	add(saveAsButton);
	add(showButton);
	add(openButton);
	add(calcola5Button);
	add(calcola3Button);
	add(ellipseButton);
	add(circellButton);
	add(separation2);
	add(boundaryButton);
	add(cancelButton);
	pack();
} /* end caFile */

} /* end class caFile */


/*====================================================================
|	caToolbar
\===================================================================*/

/*********************************************************************
 * This class deals with the toolbar that gets substituted to that of
 * ImageJ.
 ********************************************************************/
class caToolbar
	extends
		Canvas
	implements
		MouseListener

{ /* begin class caToolbar */

/*....................................................................
	Private variables
....................................................................*/
private static final int NUM_TOOLS = 19;
private static final int SIZE = 22;
private static final int OFFSET = 3;
private static final Color gray = Color.lightGray;
private static final Color brighter = gray.brighter();
private static final Color darker = gray.darker();
private static final Color evenDarker = darker.darker();
private final boolean[] down = new boolean[NUM_TOOLS];
private Graphics g;
private ImagePlus imp;
private Toolbar previousInstance;
private caHandler ph;
private caToolbar instance;
private long mouseDownTime;
private int currentTool = caAct.ADD_CROSS;
private int x;
private int y;
private int xOffset;
private int yOffset;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 * Return the index of the tool that is currently activated.
 ********************************************************************/
public int getCurrentTool (
) {
	return(currentTool);
} /* getCurrentTool */

/*********************************************************************
 * Listen to <code>mouseClicked</code> events.
 *
 * @param e Ignored.
 ********************************************************************/
public void mouseClicked (
	final MouseEvent e
) {
} /* end mouseClicked */

/*********************************************************************
 * Listen to <code>mouseEntered</code> events.
 *
 * @param e Ignored.
 ********************************************************************/
public void mouseEntered (
	final MouseEvent e
) {
} /* end mouseEntered */

/*********************************************************************
 * Listen to <code>mouseExited</code> events.
 *
 * @param e Ignored.
 ********************************************************************/
public void mouseExited (
	final MouseEvent e
) {
} /* end mouseExited */

/*********************************************************************
 * Listen to <code>mousePressed</code> events. Test for single or double
 * clicks and perform the relevant action.
 *
 * @param e Event.
 ********************************************************************/
public void mousePressed (
	final MouseEvent e
) {
	final int x = e.getX();
	final int y = e.getY();
	final int previousTool = currentTool;
	int newTool = 0;
	for (int i = 0; (i < NUM_TOOLS); i++) {
		if (((i * SIZE) < x) && (x < (i * SIZE + SIZE))) {
			newTool = i;
		}
	}
	final boolean doubleClick = ((newTool == getCurrentTool())
		&& ((System.currentTimeMillis() - mouseDownTime) <= 500L));
	mouseDownTime = System.currentTimeMillis();
	setTool(newTool);
	if (doubleClick) {
		switch (newTool) {
			case caAct.ADD_CROSS:
			case caAct.MOVE_CROSS:
				caColorSettings colorDialog
					= new caColorSettings(IJ.getInstance(), ph);
				GUI.center(colorDialog);
				colorDialog.setVisible(true);
				colorDialog.dispose();
				break;
			case caAct.REMOVE_CROSS:
				caClearAll clearAllDialog
					= new caClearAll(IJ.getInstance(), ph);
				GUI.center(clearAllDialog);
				clearAllDialog.setVisible(true);
				clearAllDialog.dispose();
				break;
		}
	}
	switch (newTool) {
		case caAct.FILE:
			caFile fileDialog
				= new caFile(IJ.getInstance(), ph, imp);
			GUI.center(fileDialog);
			fileDialog.setVisible(true);
			
			fileDialog.dispose();
			break;
		case caAct.BOTH_BEST:
			new caFile(IJ.getInstance(), ph, imp).actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Both Bestfits"));
			setTool(previousTool);
			//break; //fall through and terminate
		case caAct.TERMINATE:
			ph.cleanUp();
			restorePreviousToolbar();
			Toolbar.getInstance().repaint();
			break;
	}
} /* mousePressed */

/*********************************************************************
 * Listen to <code>mouseReleased</code> events.
 *
 * @param e Ignored.
 ********************************************************************/
public void mouseReleased (
	final MouseEvent e
) {
} /* end mouseReleased */

/*********************************************************************
 * Draw the tools of the toolbar.
 *
 * @param g Graphics environment.
 ********************************************************************/
public void paint (
	final Graphics g
) {
	for (int i = 0; (i < NUM_TOOLS); i++) {
		drawButton(g, i);
	}
} /* paint */

/*********************************************************************
 * This constructor substitutes ImageJ's toolbar by that of PointPicker_.
 *
 * @param previousToolbar ImageJ's toolbar.
 ********************************************************************/
public caToolbar (
	final Toolbar previousToolbar
) {
	previousInstance = previousToolbar;
	instance = this;
	final Container container = previousToolbar.getParent();
	final Component component[] = container.getComponents();
	for (int i = 0; (i < component.length); i++) {
		if (component[i] == previousToolbar) {
			container.remove(previousToolbar);
			container.add(this, i);
			break;
		}
	}
	resetButtons();
	down[currentTool] = true;
	setTool(currentTool);
	setForeground(evenDarker);
	setBackground(gray);
	addMouseListener(this);
	container.validate();
} /* end caToolbar */

/*********************************************************************
 * Setup the point handler.
 *
 * @param ph <code>caHandler<code> object that handles operations.
 * @param imp <code>ImagePlus<code> object where points are being picked.
 ********************************************************************/
public void setWindow (
	final caHandler ph,
	final ImagePlus imp
) {
	this.ph = ph;
	this.imp = imp;
} /* end setWindow */

/*********************************************************************
 * Setup the current tool. The selection of non-functional tools is
 * honored but leads to a no-op action.
 *
 * @param tool Admissible tools belong to [<code>0</code>,
 * <code>NUM_TOOLS - 1</code>]
 ********************************************************************/
public void setTool (
	final int tool
) {
	if (tool == currentTool) {
		return;
	}
	down[tool] = true;
	down[currentTool] = false;
	final Graphics g = this.getGraphics();
	drawButton(g, currentTool);
	drawButton(g, tool);
	g.dispose();
	showMessage(tool);
	currentTool = tool;
} /* end setTool */

/*....................................................................
	Private methods
....................................................................*/

 /*------------------------------------------------------------------*/
private void d (
	int x,
	int y
) {
	x += xOffset;
	y += yOffset;
	g.drawLine(this.x, this.y, x, y);
	this.x = x;
	this.y = y;
} /* end d */

/*------------------------------------------------------------------*/
private void drawButton (
	final Graphics g,
	final int tool
) {
	fill3DRect(g, tool * SIZE + 1, 1, SIZE, SIZE - 1, !down[tool]);
	g.setColor(Color.black);
	int x = tool * SIZE + OFFSET;
	int y = OFFSET;
	if (down[tool]) {
		x++;
		y++;
	}
	this.g = g;
	switch (tool) {
		case caAct.ADD_CROSS:
			xOffset = x;
			yOffset = y;
			m(7, 0);
			d(7, 1);
			m(6, 2);
			d(6, 3);
			m(8, 2);
			d(8, 3);
			m(5, 4);
			d(5, 5);
			m(9, 4);
			d(9, 5);
			m(4, 6);
			d(4, 8);
			m(10, 6);
			d(10, 8);
			m(5, 9);
			d(5, 14);
			m(9, 9);
			d(9, 14);
			m(7, 4);
			d(7, 6);
			m(7, 8);
			d(7, 8);
			m(4, 11);
			d(10, 11);
			g.fillRect(x + 6, y + 12, 3, 3);
			m(11, 13);
			d(15, 13);
			m(13, 11);
			d(13, 15);
			break;
		case caAct.MOVE_CROSS:
			xOffset = x;
			yOffset = y;
			m(1, 1);
			d(1, 10);
			m(2, 2);
			d(2, 9);
			m(3, 3);
			d(3, 8);
			m(4, 4);
			d(4, 7);
			m(5, 5);
			d(5, 7);
			m(6, 6);
			d(6, 7);
			m(7, 7);
			d(7, 7);
			m(11, 5);
			d(11, 6);
			m(10, 7);
			d(10, 8);
			m(12, 7);
			d(12, 8);
			m(9, 9);
			d(9, 11);
			m(13, 9);
			d(13, 11);
			m(10, 12);
			d(10, 15);
			m(12, 12);
			d(12, 15);
			m(11, 9);
			d(11, 10);
			m(11, 13);
			d(11, 15);
			m(9, 13);
			d(13, 13);
			break;
		case caAct.REMOVE_CROSS:
			xOffset = x;
			yOffset = y;
			m(7, 0);
			d(7, 1);
			m(6, 2);
			d(6, 3);
			m(8, 2);
			d(8, 3);
			m(5, 4);
			d(5, 5);
			m(9, 4);
			d(9, 5);
			m(4, 6);
			d(4, 8);
			m(10, 6);
			d(10, 8);
			m(5, 9);
			d(5, 14);
			m(9, 9);
			d(9, 14);
			m(7, 4);
			d(7, 6);
			m(7, 8);
			d(7, 8);
			m(4, 11);
			d(10, 11);
			g.fillRect(x + 6, y + 12, 3, 3);
			m(11, 13);
			d(15, 13);
			break;
		case caAct.FILE:
			xOffset = x;
			yOffset = y;
			m(3, 1);
			d(9, 1);
			d(9, 4);
			d(12, 4);
			d(12, 14);
			d(3, 14);
			d(3, 1);
			m(10, 2);
			d(11, 3);
			m(5, 4);
			d(7, 4);
			m(5, 6);
			d(10, 6);
			m(5, 8);
			d(10, 8);
			m(5, 10);
			d(10, 10);
			m(5, 12);
			d(10, 12);
			break;
		case caAct.BOTH_BEST:
			xOffset = x;
			yOffset = y;
			m(3, 1);
			d(9, 1);
			d(9, 4);
			d(12, 4);
			d(12, 14);
			d(3, 14);
			d(3, 1);
			
			break;
		case caAct.TERMINATE:
			xOffset = x;
			yOffset = y;
			m(5, 0);
			d(5, 8);
			m(4, 5);
			d(4, 7);
			m(3, 6);
			d(3, 7);
			m(2, 7);
			d(2, 9);
			m(1, 8);
			d(1, 9);
			m(2, 10);
			d(6, 10);
			m(3, 11);
			d(3, 13);
			m(1, 14);
			d(6, 14);
			m(0, 15);
			d(7, 15);
			m(2, 13);
			d(2, 13);
			m(5, 13);
			d(5, 13);
			m(7, 8);
			d(14, 8);
			m(8, 7);
			d(15, 7);
			m(8, 9);
			d(13, 9);
			m(9, 6);
			d(9, 10);
			m(15, 4);
			d(15, 6);
			d(14, 6);
			break;
		case caAct.MAGNIFIER:
			xOffset = x + 2;
			yOffset = y + 2;
			m(3, 0);
			d(3, 0);
			d(5, 0);
			d(8, 3);
			d(8, 5);
			d(7, 6);
			d(7, 7);
			d(6, 7);
			d(5, 8);
			d(3, 8);
			d(0, 5);
			d(0, 3);
			d(3, 0);
			m(8, 8);
			d(9, 8);
			d(13, 12);
			d(13, 13);
			d(12, 13);
			d(8, 9);
			d(8, 8);
			break;
	}
} /* end drawButton */

/*------------------------------------------------------------------*/
private void fill3DRect (
	final Graphics g,
	final int x,
	final int y,
	final int width,
	final int height,
	final boolean raised
) {
	if (raised) {
		g.setColor(gray);
	}
	else {
		g.setColor(darker);
	}
	g.fillRect(x + 1, y + 1, width - 2, height - 2);
	g.setColor((raised) ? (brighter) : (evenDarker));
	g.drawLine(x, y, x, y + height - 1);
	g.drawLine(x + 1, y, x + width - 2, y);
	g.setColor((raised) ? (evenDarker) : (brighter));
	g.drawLine(x + 1, y + height - 1, x + width - 1, y + height - 1);
	g.drawLine(x + width - 1, y, x + width - 1, y + height - 2);
} /* end fill3DRect */

/*------------------------------------------------------------------*/
private void m (
	final int x,
	final int y
) {
	this.x = xOffset + x;
	this.y = yOffset + y;
} /* end m */

/*------------------------------------------------------------------*/
private void resetButtons (
) {
	for (int i = 0; (i < NUM_TOOLS); i++) {
		down[i] = false;
	}
} /* end resetButtons */

/*------------------------------------------------------------------*/
public void restorePreviousToolbar (
) {
	final Container container = instance.getParent();
	final Component component[] = container.getComponents();
	for (int i = 0; (i < component.length); i++) {
		if (component[i] == instance) {
			container.remove(instance);
			container.add(previousInstance, i);
			container.validate();
			break;
		}
	}
} /* end restorePreviousToolbar */

/*------------------------------------------------------------------*/
private void showMessage (
	final int tool
) {
	switch (tool) {
		case caAct.ADD_CROSS:
			IJ.showStatus("Add crosses");
			return;
		case caAct.MOVE_CROSS:
			IJ.showStatus("Move crosses");
			return;
		case caAct.REMOVE_CROSS:
			IJ.showStatus("Remove crosses");
			return;
		case caAct.FILE:
			IJ.showStatus("PlugIn Preferences");
			return;
		case caAct.TERMINATE:
			IJ.showStatus("Exit PointPicker");
			return;
		case caAct.BOTH_BEST:
			IJ.showStatus("Run Both BestFit");
			return;
		case caAct.MAGNIFIER:
			IJ.showStatus("Magnifying glass");
			return;
		default:
			IJ.showStatus("Undefined operation");
			return;
	}
} /* end showMessage */

} /* end class caToolbar */
