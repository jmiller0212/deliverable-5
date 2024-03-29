import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.Random;

/**
 * Code by @author Wonsun Ahn
 * 
 * <p>BeanCounterLogic: The bean counter, also known as a quincunx or the Galton
 * box, is a device for statistics experiments named after English scientist Sir
 * Francis Galton. It consists of an upright board with evenly spaced nails (or
 * pegs) in a triangular form. Each bean takes a random path and falls into a
 * slot.
 *
 * <p>Beans are dropped from the opening of the board. Every time a bean hits a
 * nail, it has a 50% chance of falling to the left or to the right. The piles
 * of beans are accumulated in the slots at the bottom of the board.
 * 
 * <p>This class implements the core logic of the machine. The MainPanel uses the
 * state inside BeanCounterLogic to display on the screen.
 * 
 * <p>Note that BeanCounterLogic uses a logical coordinate system to store the
 * positions of in-flight beans.For example, for a 4-slot machine:
 *                      (0, 0)
 *               (0, 1)        (1, 1)
 *        (0, 2)        (1, 2)        (2, 2)
 *  (0, 3)       (1, 3)        (2, 3)       (3, 3)
 * [Slot0]       [Slot1]       [Slot2]      [Slot3]
 */

public class BeanCounterLogicImpl implements BeanCounterLogic {
	int slotCount;
	int beanCount;	// need a beanCount because of beans reassignment in repeat()
	int beansRemaining;
	double sum;
	Bean[] beans;
	ArrayList<Bean> beansAL;
	Bean[] beansInFlight;
	int[] beansInSlot;

	/**
	 * Constructor - creates the bean counter logic object that implements the core
	 * logic with the provided number of slots.
	 * 
	 * @param slotCount the number of slots in the machine
	 */
	BeanCounterLogicImpl(int slotCount) {
		this.slotCount = slotCount;
		this.beansInSlot = new int[slotCount];
		for (int i = 0; i < slotCount; i++) {
			beansInSlot[i] = 0;
		}
		this.beansInFlight = new Bean[slotCount];
		this.beansAL = new ArrayList<>();
		this.sum = 0;
	}
	
	public static class BeanComparator implements Comparator<Bean>, Serializable {
		/**
		 * I guess this is necessary.
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * compare - allows for ArrayList sorting.
		 */
		public int compare(Bean b1, Bean b2) {
			if (b1.getXPos() == b2.getXPos()) {
				return 0;
			} else if (b1.getXPos() > b2.getXPos()) {
				return 1;
			} else {
				return -1;
			}
		}
	}

	/**
	 * Returns the number of slots the machine was initialized with.
	 * 
	 * @return number of slots
	 */
	public int getSlotCount() {
		return slotCount;
	}
	
	/**
	 * Returns the number of beans remaining that are waiting to get inserted.
	 * 
	 * @return number of beans remaining
	 */
	public int getRemainingBeanCount() {
		return beansRemaining;
	}

	/**
	 * Returns the x-coordinate for the in-flight bean at the provided y-coordinate.
	 * 
	 * @param yPos the y-coordinate in which to look for the in-flight bean
	 * @return the x-coordinate of the in-flight bean; if no bean in y-coordinate, return NO_BEAN_IN_YPOS
	 */
	public int getInFlightBeanXPos(int yPos) {
		return beansInFlight[yPos] != null ? beansInFlight[yPos].getXPos() : NO_BEAN_IN_YPOS;
	}

	/**
	 * Returns the number of beans in the ith slot.
	 * 
	 * @param i index of slot
	 * @return number of beans in slot
	 */
	public int getSlotBeanCount(int i) {
		return i >= 0 && i < slotCount ? beansInSlot[i] : -1;
	}

	/**
	 * Calculates the average slot number of all the beans in slots.
	 * 
	 * @return Average slot number of all the beans in slots.
	 */
	public double getAverageSlotBeanCount() {
		int beansInSlotCount = 0;
		for (int i = 0; i < slotCount; i++) {
			beansInSlotCount += getSlotBeanCount(i);
		}
		if (beanCount == 0 || beansInSlotCount == 0) {
			return 0;
		}
		return sum / beansInSlotCount;
	}

	/**
	 * Removes the lower half of all beans currently in slots, keeping only the
	 * upper half. If there are an odd number of beans, remove (N-1)/2 beans, where
	 * N is the number of beans. So, if there are 3 beans, 1 will be removed and 2
	 * will be remaining.
	 */
	public void upperHalf() {
		// the number of beans we have to remove
		int halfToRemove = 0;
		
		// even
		if (beansAL.size() % 2 == 0) {
			halfToRemove = beansAL.size() / 2;
		} else {	// odd
			halfToRemove = (beansAL.size() - 1) / 2;
		}
		
		Collections.sort(beansAL, new BeanComparator());
		
		for (int i = 0; i < beansAL.size() - 1; i++) {
			// remove it while halfToRemove is greater than 0
			if (halfToRemove > 0) {
				Bean b = beansAL.remove(0);
				beansInSlot[b.getXPos()]--;
				sum -= b.getXPos();
				b.reset();
				halfToRemove--;
			} else {
				break;
			}
		}
	}

	/**
	 * Removes the upper half of all beans currently in slots, keeping only the
	 * lower half.  If there are an odd number of beans, remove (N-1)/2 beans, where
	 * N is the number of beans. So, if there are 3 beans, 1 will be removed and 2
	 * will be remaining.
	 */
	public void lowerHalf() {
		// the number of beans we have to remove
		int halfToRemove = 0;
		
		// even
		if (beansAL.size() % 2 == 0) {
			halfToRemove = beansAL.size() / 2;
		} else {	// odd
			halfToRemove = (beansAL.size() - 1) / 2;
		}
		
		Collections.sort(beansAL, new BeanComparator());
		
		// for each bean in the slot array list
		for (int i = beansAL.size() - 1; i >= 0; i--) {
			// remove it while halfToRemove is greater than 0
			if (halfToRemove > 0) {
				Bean b = beansAL.remove(i);
				beansInSlot[b.getXPos()]--;
				sum -= b.getXPos();
				b.reset();
				halfToRemove--;
			} else {
				break;
			}
		}
	}
	
	/**
	 * A hard reset. Initializes the machine with the passed beans. The machine
	 * starts with one bean at the top.
	 * 
	 * @param beans array of beans to add to the machine
	 */
	public void reset(Bean[] beans) {
		this.beans = beans.clone();
		beanCount = beans.length;
		beansAL.clear();
		sum = 0;
		
		for (Bean b : beans) {
			if (b != null) {
				b.reset();
			}
		}
		
		// we can get the number of beans remaining
		if (beans.length > 0) {
			beansRemaining = beans.length - 1;
		} else {
			beansRemaining = 0;
		}
		
		if (beans.length > 0) {
			// initialize beansInFlight
			for (int i = 0; i < slotCount; i++) {
				if (i == 0) {
					// first bean is initialized
					beansInFlight[i] = beans[0];
				} else {
					beansInFlight[i] = null;
				}
			}
		}

		// zero out the beans in-slot
		for (int i = 0; i < slotCount; i++) {
			beansInSlot[i] = 0;
		}
	}

	/**
	 * Repeats the experiment by scooping up all beans in the slots and all beans
	 * in-flight and adding them into the pool of remaining beans. As in the
	 * beginning, the machine starts with one bean at the top.
	 */
	public void repeat() {
		// reset bean count
		sum = 0;
		// reset all beans
		for (Bean b : beans) {
			if (b != null) {
				b.reset();
			}
		}
		
		// save reference of any remaining beans
		ArrayList<Bean> remaining = new ArrayList<>();
		while (beansRemaining > 0) {
			Bean b = getNextBean();
			if (b != null) {
				remaining.add(b);
			}
		}
		// beanCount re-initialized to zero here since it is used in getNextBean()
		beanCount = 0;
		
		// scoop up slot beans
		if (beansAL.size() > 0) {
			for (int i = beansAL.size() - 1; i >= 0; i--) {
				beans[beanCount] = beansAL.remove(i);
				beanCount++;
			}
		}
		
		// and in flight beans
		for (int i = beansInFlight.length - 1; i >= 0; i--) {
			if (beansInFlight[i] != null) {
				beans[beanCount] = beansInFlight[i];
				beanCount++;
			}
		}
		
		// and finally the remaining beans
		if (remaining.size() > 0) {
			for (int i = remaining.size() - 1; i >= 0; i--) {
				beans[beanCount] = remaining.remove(i);
				beanCount++;
			}
		}
		
		// reset the number of beans remaining
		if (beanCount > 0) {
			beansRemaining = beanCount - 1;
		} else {
			beansRemaining = 0;
		}

		if (beanCount > 0) {
			// reinitialize beansInFlight
			for (int i = 0; i < slotCount; i++) {
				if (i == 0) {
					// first bean is initialized
					beansInFlight[i] = beans[0];
				} else {
					beansInFlight[i] = null;
				}
			}
		}
		
		// zero out the beans in-slot
		for (int i = 0; i < slotCount; i++) {
			beansInSlot[i] = 0;
		}
	}

	/**
	 * Advances the machine one step. All the in-flight beans fall down one step to
	 * the next peg. A new bean is inserted into the top of the machine if there are
	 * beans remaining.
	 * 
	 * @return whether there has been any status change. If there is no change, that
	 *         means the machine is finished.
	 */
	public boolean advanceStep() {
		boolean statusChange = false;

		// if an in-flight bean is ready to be slotted
		if (beansInFlight[slotCount - 1] != null) {
			statusChange = true;
			// increment number of beans in that position
			beansInSlot[beansInFlight[slotCount - 1].getXPos()]++;
			beansAL.add(beansInFlight[slotCount - 1]);
			sum += beansInFlight[slotCount - 1].getXPos();
		}

		for (int i = beansInFlight.length - 1; i > 0; i--) {
			if (beansInFlight[i - 1] != null) {
				statusChange = true;
				beansInFlight[i - 1].choose();
				beansInFlight[i] = beansInFlight[i - 1];
			} else {
				beansInFlight[i] = null;
			}
		}
		Bean nextBean = getNextBean();
		if (nextBean != null) {
			statusChange = true;
			beansInFlight[0] = nextBean;
		} else {
			beansInFlight[0] = null;
		}
		return statusChange;
	}
	
	private Bean getNextBean() {
		int nextIndex = beanCount - beansRemaining;
		if (beansRemaining > 0) {
			beansRemaining--;
		}
		return nextIndex < beanCount ? beans[nextIndex] : null;
	}
	
	/**
	 * Number of spaces in between numbers when printing out the state of the machine.
	 * Make sure the number is odd (even numbers don't work as well).
	 */
	private int xspacing = 3;

	/**
	 * Calculates the number of spaces to indent for the given row of pegs.
	 * 
	 * @param yPos the y-position (or row number) of the pegs
	 * @return the number of spaces to indent
	 */
	private int getIndent(int yPos) {
		int rootIndent = (getSlotCount() - 1) * (xspacing + 1) / 2 + (xspacing + 1);
		return rootIndent - (xspacing + 1) / 2 * yPos;
	}

	/**
	 * Constructs a string representation of the bean count of all the slots.
	 * 
	 * @return a string with bean counts for each slot
	 */
	public String getSlotString() {
		StringBuilder bld = new StringBuilder();
		Formatter fmt = new Formatter(bld);
		String format = "%" + (xspacing + 1) + "d";
		for (int i = 0; i < getSlotCount(); i++) {
			fmt.format(format, getSlotBeanCount(i));
		}
		fmt.close();
		return bld.toString();
	}

	/**
	 * Constructs a string representation of the entire machine. If a peg has a bean
	 * above it, it is represented as a "1", otherwise it is represented as a "0".
	 * At the very bottom is attached the slots with the bean counts.
	 * 
	 * @return the string representation of the machine
	 */
	public String toString() {
		StringBuilder bld = new StringBuilder();
		Formatter fmt = new Formatter(bld);
		for (int yPos = 0; yPos < getSlotCount(); yPos++) {
			int xBeanPos = getInFlightBeanXPos(yPos);
			for (int xPos = 0; xPos <= yPos; xPos++) {
				int spacing = (xPos == 0) ? getIndent(yPos) : (xspacing + 1);
				String format = "%" + spacing + "d";
				if (xPos == xBeanPos) {
					fmt.format(format, 1);
				} else {
					fmt.format(format, 0);
				}
			}
			fmt.format("%n");
		}
		fmt.close();
		return bld.toString() + getSlotString();
	}

	/**
	 * Prints usage information.
	 */
	public static void showUsage() {
		System.out.println("Usage: java BeanCounterLogic slot_count bean_count <luck | skill> [debug]");
		System.out.println("Example: java BeanCounterLogic 10 400 luck");
		System.out.println("Example: java BeanCounterLogic 20 1000 skill debug");
	}
	
	/**
	 * Auxiliary main method. Runs the machine in text mode with no bells and
	 * whistles. It simply shows the slot bean count at the end.
	 * 
	 * @param args commandline arguments; see showUsage() for detailed information
	 */
	public static void main(String[] args) {
		boolean debug;
		boolean luck;
		int slotCount = 0;
		int beanCount = 0;

		if (args.length != 3 && args.length != 4) {
			showUsage();
			return;
		}

		try {
			slotCount = Integer.parseInt(args[0]);
			beanCount = Integer.parseInt(args[1]);
		} catch (NumberFormatException ne) {
			showUsage();
			return;
		}
		if (beanCount < 0) {
			showUsage();
			return;
		}

		if (args[2].equals("luck")) {
			luck = true;
		} else if (args[2].equals("skill")) {
			luck = false;
		} else {
			showUsage();
			return;
		}
		
		if (args.length == 4 && args[3].equals("debug")) {
			debug = true;
		} else {
			debug = false;
		}

		// Create the internal logic
		BeanCounterLogicImpl logic = new BeanCounterLogicImpl(slotCount);
		// Create the beans (in luck mode)
		BeanImpl[] beans = new BeanImpl[beanCount];
		for (int i = 0; i < beanCount; i++) {
			beans[i] = new BeanImpl(slotCount, luck, new Random());
		}
		// Initialize the logic with the beans
		logic.reset(beans);

		if (debug) {
			System.out.println(logic.toString());
		}

		// Perform the experiment
		while (true) {
			if (!logic.advanceStep()) {
				break;
			}
			if (debug) {
				System.out.println(logic.toString());
			}
		}
		// display experimental results
		System.out.println("Slot bean counts:");
		System.out.println(logic.getSlotString());
	}
}
