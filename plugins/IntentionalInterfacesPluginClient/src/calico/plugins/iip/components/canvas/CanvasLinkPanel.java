package calico.plugins.iip.components.canvas;

import it.unimi.dsi.fastutil.longs.Long2ReferenceArrayMap;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.SwingUtilities;

import calico.Calico;
import calico.controllers.CCanvasController;
import calico.inputhandlers.CalicoAbstractInputHandler;
import calico.inputhandlers.CalicoInputManager;
import calico.inputhandlers.InputEventInfo;
import calico.inputhandlers.StickyItem;
import calico.plugins.iip.components.CCanvasLink;
import calico.plugins.iip.components.CCanvasLinkAnchor;
import calico.plugins.iip.components.CIntentionCell;
import calico.plugins.iip.components.CIntentionType;
import calico.plugins.iip.components.IntentionPanelLayout;
import calico.plugins.iip.components.piemenu.DeleteLinkButton;
import calico.plugins.iip.components.piemenu.SetLinkLabelButton;
import calico.plugins.iip.controllers.CCanvasLinkController;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.controllers.IntentionCanvasController;
import calico.plugins.iip.iconsets.CalicoIconManager;
import calico.plugins.iip.util.IntentionalInterfacesGraphics;
import edu.umd.cs.piccolo.nodes.PImage;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolox.nodes.PComposite;

public class CanvasLinkPanel implements StickyItem
{
	public static CanvasLinkPanel getInstance()
	{
		return INSTANCE;
	}

	private static CanvasLinkPanel INSTANCE = new CanvasLinkPanel();

	public static final double PANEL_INSET_X = 100.0;
	public static final double PANEL_INSET_Y = 50.0;
	public static final double PANEL_COMPONENT_INSET = 3.0;

	public static final double TABLE_UNIT_SPAN = 20.0;
	public static final double ROW_TEXT_INSET = 1.0;

	private static final Color PREVIEW_ROW_BACKGROUND = new Color(0xEAEAEA);

	final PanelNode panel = new PanelNode();

	private final long uuid;
	private long canvas_uuid;
	// remove this
	private final CCanvasLink.LinkDirection direction = CCanvasLink.LinkDirection.INCOMING;

	private boolean visible;
	private IntentionPanelLayout layout;
	private final List<CCanvasLinkToken> tokens = new ArrayList<CCanvasLinkToken>();

	private final DeleteLinkButton deleteLinkButton = new DeleteLinkButton();
	private final SetLinkLabelButton setLinkLabelButton = new SetLinkLabelButton();

	private final Image checkmarkImage;
	private final Image linkFrameImage;
	private final Dimension tableCheckmarkInset = new Dimension();

	private boolean initialized = false;

	private CanvasLinkPanel()
	{
		uuid = Calico.uuid();
		canvas_uuid = 0L;

		CalicoInputManager.addCustomInputHandler(uuid, new InputHandler());

		checkmarkImage = CalicoIconManager.getIconImage("intention.checkmark");
		linkFrameImage = CalicoIconManager.getIconImage("intention.link-frame");
		tableCheckmarkInset.width = (int) ((TABLE_UNIT_SPAN - checkmarkImage.getWidth(null)) / 2.0);
		tableCheckmarkInset.height = (int) ((TABLE_UNIT_SPAN - checkmarkImage.getHeight(null)) / 2.0);

		panel.setVisible(visible = false);
		panel.initialize();

		initialized = true;
	}

	@Override
	public long getUUID()
	{
		return uuid;
	}

	@Override
	public boolean containsPoint(Point p)
	{
		return panel.getBounds().contains(p);
	}

	public boolean isVisible()
	{
		return panel.getVisible();
	}

	public void setVisible(boolean b)
	{
		if (visible == b)
		{
			return;
		}

		visible = b;

		if (b)
		{
			refresh();
		}
		else
		{
			panel.setVisible(false);
		}

		if (b)
		{
			CalicoInputManager.registerStickyItem(this);
			panel.repaint();
		}
		else
		{
			CalicoInputManager.unregisterStickyItem(this);
		}
	}

	public void moveTo(long canvas_uuid)
	{
		this.canvas_uuid = canvas_uuid;

		if (panel.getParent() != null)
		{
			panel.getParent().removeChild(panel);
		}
		updateLinks();
		CCanvasController.canvasdb.get(canvas_uuid).getCamera().addChild(panel);
	}

	public void refresh()
	{
		if (!visible)
		{
			return;
		}

		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable() {
				public void run()
				{
					refresh();
				}
			});
			return;
		}

		panel.refreshIntentionTypeSelections();
		panel.setVisible(true);
		updatePanelBounds();
	}

	public void updateLinks()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable() {
				public void run()
				{
					updateLinks();
				}
			});
			return;
		}

		panel.updateLinks();
		updatePanelBounds();
	}

	public void updateIntentionTypes()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable() {
				public void run()
				{
					updateIntentionTypes();
				}
			});
			return;
		}

		panel.updateIntentionTypes();
		updatePanelBounds();
	}

	private void updatePanelBounds()
	{
		double width = panel.calculateWidth();
		double height = panel.calculateHeight();
		layout.updateBounds(panel, width, height);

		if (visible)
		{
			panel.repaint();
		}
	}

	public void setLayout(IntentionPanelLayout layout)
	{
		this.layout = layout;
	}

	private class IntentionTypeRow
	{
		private final CIntentionType type;
		private final PText label;

		private double y;

		IntentionTypeRow(CIntentionType type)
		{
			this.type = type;

			label = new PText(type.getName());
			label.setConstrainWidthToTextWidth(true);
			label.setConstrainHeightToTextHeight(true);
		}

		void setPosition(double x, double y)
		{
			this.y = y;
			label.setX(x + PANEL_COMPONENT_INSET);
			label.setY(y + ROW_TEXT_INSET);
		}

		void installComponents()
		{
			panel.addChild(label);
		}

		void removeAllComponents()
		{
			panel.removeChild(label);
		}
	}

	private class LinkColumn
	{
		private final CCanvasLinkAnchor linkOppositeAnchor;
		private final PImage headerCell;

		private final Long2ReferenceArrayMap<PImage> checkmarksByIntentionTypeId = new Long2ReferenceArrayMap<PImage>();

		private int x;

		LinkColumn(CCanvasLinkAnchor linkOppositeAnchor)
		{
			this.linkOppositeAnchor = linkOppositeAnchor;

			headerCell = new PImage(IntentionalInterfacesGraphics.superimposeCellAddress(linkFrameImage, linkOppositeAnchor.getCanvasId()));
			headerCell.setBounds(0.0, 0.0, TABLE_UNIT_SPAN, TABLE_UNIT_SPAN);
		}

		void setPosition(double x, double y)
		{
			this.x = (int) x;

			headerCell.setX(x);
			headerCell.setY(y);

			for (IntentionTypeRow row : panel.typeRows)
			{
				PImage checkmark = checkmarksByIntentionTypeId.get(row.type.getId());
				if (checkmark != null)
				{
					checkmark.setX(x + tableCheckmarkInset.width);
					checkmark.setY(row.y + tableCheckmarkInset.height);
				}
			}
		}

		void installComponents()
		{
			panel.addChild(headerCell);
			updateCheckmarks();
		}

		void removeAllComponents()
		{
			panel.removeChild(headerCell);
			removeCheckmarks();
		}

		void removeCheckmarks()
		{
			for (PImage checkmark : checkmarksByIntentionTypeId.values())
			{
				panel.removeChild(checkmark);
			}
		}

		void updateIntentionTypes()
		{
			removeCheckmarks();
			checkmarksByIntentionTypeId.clear();
			updateCheckmarks();
		}

		void updateCheckmarks()
		{
			CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(linkOppositeAnchor.getCanvasId());
			for (CIntentionType type : IntentionCanvasController.getInstance().getActiveIntentionTypes())
			{
				if (cell.hasIntentionType(type.getId()))
				{
					PImage checkmark = new PImage(checkmarkImage);
					checkmarksByIntentionTypeId.put(type.getId(), checkmark);
					panel.addChild(checkmark);
				}
			}
		}
	}

	private class PreviewRow
	{
		private final PText label = new PText("Preview");

		int y;

		public PreviewRow()
		{
			label.setConstrainWidthToTextWidth(true);
			label.setConstrainHeightToTextHeight(true);
		}

		void installComponents()
		{
			panel.addChild(label);
		}

		void setPosition(double x, double y)
		{
			this.y = (int) y;

			label.setX(x + PANEL_COMPONENT_INSET);
			label.setY(y + ROW_TEXT_INSET);
		}
	}

	private class PanelNode extends PComposite
	{
		private final Color CLICK_HIGHLIGHT = new Color(0xFFFF30);
		private final Color CONTEXT_HIGHLIGHT = Color.red;

		private PPath clickHighlight = createHighlight(CLICK_HIGHLIGHT);
		private PPath contextHighlight = createHighlight(CONTEXT_HIGHLIGHT);

		private List<IntentionTypeRow> typeRows = new ArrayList<IntentionTypeRow>();
		private List<LinkColumn> linkColumns = new ArrayList<LinkColumn>();
		private final PreviewRow previewRow = new PreviewRow();

		private int xColumnStart;

		private PPath border;

		public PanelNode()
		{
			addChild(contextHighlight);
			addChild(clickHighlight);
		}

		void initialize()
		{
			panel.setPaint(Color.white);
			previewRow.installComponents();
		}

		private PPath createHighlight(Color c)
		{
			PPath highlight = new PPath(new Rectangle2D.Double(0, 0, CCanvasLinkToken.TOKEN_WIDTH, CCanvasLinkToken.TOKEN_HEIGHT));
			highlight.setStrokePaint(c);
			highlight.setStroke(new BasicStroke(1f));
			highlight.setVisible(false);
			return highlight;
		}

		private double getMaxIntentionHeaderWidth()
		{
			double maxWidth = 0.0;
			for (IntentionTypeRow row : typeRows)
			{
				if (row.label.getBounds().width > maxWidth)
				{
					maxWidth = row.label.getBounds().width;
				}
			}
			return maxWidth;
		}

		private double calculateWidth()
		{
			return getMaxIntentionHeaderWidth() + (TABLE_UNIT_SPAN * linkColumns.size()) + (2 * PANEL_COMPONENT_INSET);
		}

		private double calculateHeight()
		{
			return (2 + typeRows.size()) * TABLE_UNIT_SPAN;
		}

		void refreshIntentionTypeSelections()
		{
			if (canvas_uuid == 0L)
			{
				return;
			}

			for (LinkColumn column : linkColumns)
			{
				column.updateIntentionTypes();
			}
		}

		void updateIntentionTypes()
		{
			for (IntentionTypeRow row : typeRows)
			{
				row.removeAllComponents();
			}
			typeRows.clear();

			for (CIntentionType type : IntentionCanvasController.getInstance().getActiveIntentionTypes())
			{
				IntentionTypeRow row = new IntentionTypeRow(type);
				row.installComponents();
				typeRows.add(row);
			}

			refreshIntentionTypeSelections();

			if (border != null)
			{
				removeChild(border);
			}
			border = new PPath(new Rectangle2D.Double(0, 0, calculateWidth(), calculateHeight()));
			border.setStrokePaint(Color.black);
			border.setStroke(new BasicStroke(1f));
			addChild(border);
		}

		void updateLinks()
		{
			for (LinkColumn column : linkColumns)
			{
				column.removeAllComponents();
			}
			linkColumns.clear();
			// TODO: sort us
			for (long anchorId : CCanvasLinkController.getInstance().getAnchorIdsByCanvasId(canvas_uuid))
			{
				LinkColumn column = new LinkColumn(CCanvasLinkController.getInstance().getAnchor(anchorId).getOpposite());
				linkColumns.add(column);
				column.installComponents();
			}
		}

		LinkColumn getClickedPreviewCell(InputEventInfo event)
		{
			if (event.getY() < previewRow.y)
			{
				return null;
			}

			if (event.getX() < xColumnStart)
			{
				return null;
			}

			for (LinkColumn column : linkColumns)
			{
				if ((column.x + TABLE_UNIT_SPAN) > event.getX())
				{
					panel.highlightClickedColumn(column);
					return column;
				}
			}

			return null;
		}

		private void highlightClickedColumn(LinkColumn column)
		{
			showHighlight(clickHighlight, column);
			repaint();
		}

		private void showHighlight(PPath highlight, LinkColumn column)
		{
			PBounds bounds = getBounds();
			highlight.setBounds(column.x, bounds.getY(), TABLE_UNIT_SPAN, TABLE_UNIT_SPAN);
			highlight.moveToFront();
			highlight.setVisible(true);
		}

		@Override
		protected void layoutChildren()
		{
			if (!initialized)
			{
				return;
			}

			PBounds bounds = panel.getBoundsReference();

			double yRow = bounds.y;
			for (IntentionTypeRow row : typeRows)
			{
				row.setPosition(bounds.x, yRow += TABLE_UNIT_SPAN);
			}
			previewRow.setPosition(bounds.x, yRow += TABLE_UNIT_SPAN);

			double xColumn = bounds.x + getMaxIntentionHeaderWidth() + PANEL_COMPONENT_INSET;
			this.xColumnStart = (int) xColumn;
			for (LinkColumn column : linkColumns)
			{
				column.setPosition(xColumn, bounds.y);
				xColumn += TABLE_UNIT_SPAN;
			}

			if (CCanvasLinkController.getInstance().hasTraversedLink())
			{
				long traversedCanvasId = CCanvasLinkController.getInstance().getTraversedLinkSourceCanvas();
				for (LinkColumn column : linkColumns)
				{
					if (column.linkOppositeAnchor.getCanvasId() == traversedCanvasId)
					{
						showHighlight(contextHighlight, column);
						break;
					}
				}
			}
			else
			{
				contextHighlight.setVisible(false);
			}

			border.setBounds(bounds);
		}
		
		@Override
		protected void paint(PPaintContext paintContext)
		{
			super.paint(paintContext);

			Graphics2D g = paintContext.getGraphics();
			Color c = g.getColor();

			PBounds bounds = getBounds();
			g.setColor(PREVIEW_ROW_BACKGROUND);
			g.fillRect((int) bounds.x, previewRow.y, (int) bounds.width, (int) TABLE_UNIT_SPAN);

			g.setColor(c);
		}
	}

	private enum InputState
	{
		IDLE,
		PRESSED,
		THUMBNAIL
	}

	private class InputHandler extends CalicoAbstractInputHandler
	{
		private final Object stateLock = new Object();

		private InputState state = InputState.IDLE;
		private LinkColumn clickedPreviewCell = null;

		private final long tapDuration = 500L;

		private final PressAndHoldTimer pressAndHold = new PressAndHoldTimer();

		@Override
		public void actionReleased(InputEventInfo event)
		{
			synchronized (stateLock)
			{
				if ((state == InputState.PRESSED) && (clickedPreviewCell != null))
				{
					CCanvasLinkController.getInstance().traverseLinkToCanvas(clickedPreviewCell.linkOppositeAnchor.getOpposite());
				}
				state = InputState.IDLE;
			}

			panel.clickHighlight.setVisible(false);
			clickedPreviewCell = null;

			CalicoInputManager.unlockHandlerIfMatch(uuid);
		}

		@Override
		public void actionDragged(InputEventInfo event)
		{
			synchronized (stateLock)
			{
				state = InputState.IDLE;
			}

			clickedPreviewCell = null;
		}

		@Override
		public void actionPressed(InputEventInfo event)
		{
			synchronized (stateLock)
			{
				state = InputState.PRESSED;
			}

			LinkColumn clickedColumn = panel.getClickedPreviewCell(event);
			if (clickedColumn != null)
			{
				clickedPreviewCell = clickedColumn;
			}

			if (clickedPreviewCell != null)
			{
				pressAndHold.start(event.getGlobalPoint());
			}
		}

		private class PressAndHoldTimer extends Timer
		{
			private Point point;

			void start(Point point)
			{
				this.point = point;

				synchronized (stateLock)
				{
					schedule(new Task(), 500L);
				}
			}

			private class Task extends TimerTask
			{
				@Override
				public void run()
				{
					synchronized (stateLock)
					{
						if (state == InputState.PRESSED)
						{
							System.out.println("Show thumbnail for canvas " + clickedPreviewCell.linkOppositeAnchor.getCanvasId());

							state = InputState.THUMBNAIL;
						}
					}
				}
			}
		}
	}
}
