package calico.controllers;

import java.util.ArrayList;
import java.util.List;

public class CHistoryController
{
	public static CHistoryController getInstance()
	{
		return INSTANCE;
	}

	public static void setup()
	{
		INSTANCE = new CHistoryController();
	}

	private static CHistoryController INSTANCE;

	private final NavigationAction BACK = new BackNavigationAction();
	private final NavigationAction FORWARD = new ForwardNavigationAction();
	private final IdleNavigationAction IDLE = new IdleNavigationAction();

	// monad representing the current operational state of this controller
	private NavigationAction navigationAction = IDLE;

	// the history stack
	private History history = new History();

	public void back()
	{
		execute(BACK);
	}

	public void forward()
	{
		execute(FORWARD);
	}

	public void push(Frame frame)
	{
		execute(new CreateFrameAction(frame));
	}

	public void purgeFrames(FrameSelector selector)
	{
		execute(new PurgeFramesAction(selector));
	}

	// all actions are serialized here to prevent integrity problems
	private synchronized void execute(NavigationAction action)
	{
		if (!navigationAction.allowsNavigation())
		{
			// avoid creating history entries during history navigation
			return;
		}
		if (!action.isValid())
		{
			System.out.println("Skipping invalid action " + action.getClass().getSimpleName());
			return;
		}

		try
		{
			this.navigationAction = action;
			this.navigationAction.execute();
		}
		finally
		{
			this.navigationAction = IDLE;
		}
	}

	private static class History
	{
		private List<Frame> frames = new ArrayList<Frame>();
		private int index = -1;

		Frame back()
		{
			if (index <= 0)
			{
				throw new IllegalStateException("Can't go back because there are no more frames to go back to.");
			}

			index--;
			return frames.get(index);
		}

		Frame forward()
		{
			if (index >= (frames.size() - 1))
			{
				throw new IllegalStateException("Can't go foward because there are no more frames to go forward to.");
			}

			index++;
			return frames.get(index);
		}

		boolean hasBack()
		{
			return index > 0;
		}

		boolean hasForward()
		{
			return index < (frames.size() - 1);
		}

		void push(Frame frame)
		{
			for (int i = (frames.size() - 1); i > index; i--)
			{
				frames.remove(i);
			}
			frames.add(frame);
			index++;
		}

		void purge(FrameSelector selector)
		{
			for (int i = (frames.size() - 1); i >= 0; i--)
			{
				if (selector.match(frames.get(i)))
				{
					frames.remove(i);
				}
			}

			if (index >= frames.size())
			{
				index = frames.size() - 1;
			}
		}
	}

	public static abstract class Frame
	{
		protected abstract void restore();
	}

	public interface FrameSelector
	{
		boolean match(Frame frame);
	}

	private abstract class NavigationAction
	{
		abstract void execute();

		boolean allowsNavigation()
		{
			return false;
		}

		boolean isValid()
		{
			return false;
		}
	}

	private final class CreateFrameAction extends NavigationAction
	{
		private final Frame frame;

		public CreateFrameAction(Frame frame)
		{
			this.frame = frame;
		}

		@Override
		void execute()
		{
			history.push(frame);
		}

		@Override
		boolean isValid()
		{
			return true;
		}
	}

	private final class PurgeFramesAction extends NavigationAction
	{
		private final FrameSelector selector;

		PurgeFramesAction(FrameSelector selector)
		{
			this.selector = selector;
		}

		@Override
		void execute()
		{
			history.purge(selector);
		}

		@Override
		boolean isValid()
		{
			return true;
		}
	}

	private final class BackNavigationAction extends NavigationAction
	{
		@Override
		void execute()
		{
			history.back().restore();
		}

		@Override
		boolean isValid()
		{
			return history.hasBack();
		}
	}

	private final class ForwardNavigationAction extends NavigationAction
	{
		@Override
		void execute()
		{
			history.forward().restore();
		}

		@Override
		boolean isValid()
		{
			return history.hasForward();
		}
	}

	private final class IdleNavigationAction extends NavigationAction
	{
		@Override
		boolean allowsNavigation()
		{
			return true;
		}

		@Override
		void execute()
		{
			throw new UnsupportedOperationException("Can't execute the idle navigation action!");
		}
	}
}
