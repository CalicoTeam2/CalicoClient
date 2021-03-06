package calico.plugins.iip.components;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import calico.Geometry;
import calico.components.arrow.AbstractArrow;
import calico.plugins.iip.components.CCanvasLinkAnchor.ArrowEndpointType;
import calico.plugins.iip.util.IntentionalInterfacesGraphics;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * Represents a <code>CCanvasLink</code> in the Piccolo component hierarchy of the Intention View. May have a label in
 * the middle of the arrow stem.
 * 
 * @author Byron Hawkins
 */
public class CCanvasLinkArrow extends AbstractArrow<CCanvasLinkAnchor>
{
	public static final Color NORMAL_COLOR = Color.black;
	public static final Color HIGHLIGHTED_COLOR = new Color(0xFFFF30);
	public static final Color FLOATING_COLOR = new Color(0x888888);

	private final CCanvasLink link;

	public CCanvasLinkArrow(CCanvasLink link)
	{
		super(Color.black, TYPE_NORM_HEAD_B);

		this.link = link;

		setAnchorA(link.getAnchorA());
		setAnchorB(link.getAnchorB());

		setHighlighted(false);
	}

	public long getId()
	{
		return link.getId();
	}

	public void setHighlighted(boolean b)
	{
		if (b)
		{
			setColor(HIGHLIGHTED_COLOR);
		}
		else
		{
			if ((link.getAnchorA().getArrowEndpointType() == ArrowEndpointType.FLOATING)
					|| (link.getAnchorB().getArrowEndpointType() == ArrowEndpointType.FLOATING))
			{
				setColor(FLOATING_COLOR);
			}
			else
			{
				setColor(NORMAL_COLOR);
			}
		}

		redraw();
	}

	@Override
	protected void addRenderingElements()
	{
		super.addRenderingElements();

		PText label = IntentionalInterfacesGraphics.createLabelOnSegment(link.getLabel(), link.getAnchorA().getPoint(), link.getAnchorB().getPoint());
		label.setTextPaint(getColor());
		addChild(0, label);
	}
}
