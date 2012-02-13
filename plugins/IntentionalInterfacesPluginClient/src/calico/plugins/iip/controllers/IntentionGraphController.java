package calico.plugins.iip.controllers;

import it.unimi.dsi.fastutil.longs.Long2ReferenceArrayMap;
import calico.Calico;
import calico.controllers.CCanvasController;
import calico.plugins.iip.components.CCanvasLink;
import calico.plugins.iip.components.CCanvasLinkArrow;
import calico.plugins.iip.components.CIntentionCell;
import calico.plugins.iip.components.graph.IntentionGraph;

public class IntentionGraphController 
{
	public static IntentionGraphController getInstance()
	{
		return INSTANCE;
	}

	public static void initialize()
	{
		INSTANCE = new IntentionGraphController();
	}

	private static IntentionGraphController INSTANCE;

	private final Long2ReferenceArrayMap<CCanvasLinkArrow> arrows = new Long2ReferenceArrayMap<CCanvasLinkArrow>();

	public void addLink(CCanvasLink link)
	{
		CCanvasLinkArrow arrow = new CCanvasLinkArrow(link);
		arrows.put(arrow.getId(), arrow);
	}

	public CCanvasLinkArrow getArrowById(long uuid)
	{
		return arrows.get(uuid);
	}

	public void removeArrowById(long uuid)
	{
		arrows.remove(uuid);
	}
	
	public void contentChanged(long canvas_uuid)
	{
		boolean hasContent = CCanvasController.hasContent(canvas_uuid);
		CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(canvas_uuid);
		
		if (cell == null)
		{
			return;
		}
		
		cell.contentsChanged();
		
		if (hasContent != cell.isVisible())
		{
			CIntentionCellController.getInstance().getCellByCanvasId(canvas_uuid).setVisible(hasContent);
		}
	}
	
	public void prepareDisplay()
	{
		IntentionGraph.getInstance().fitContents();
	}
}
