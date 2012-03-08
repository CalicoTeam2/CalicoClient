package calico.plugins.iip.components.piemenu.canvas;

import calico.components.piemenu.PieMenuButton;
import calico.controllers.CGroupController;
import calico.plugins.iip.components.CCanvasLink;
import calico.plugins.iip.controllers.CCanvasLinkController;

public class CreateDesignInsideLinkButton extends PieMenuButton
{
	public static int SHOWON = 0;

	private final long group_uuid;

	public CreateDesignInsideLinkButton(long group_uuid)
	{
		super(CCanvasLink.LinkType.DESIGN_INSIDE.image);

		this.group_uuid = group_uuid;
	}

	@Override
	public void onClick()
	{
		System.out.println("Design inside the active scrap");

		CCanvasLinkController.getInstance().createDesignInsideLink(group_uuid);
	}
}