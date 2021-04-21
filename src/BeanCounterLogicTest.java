import gov.nasa.jpf.vm.Verify;
import java.util.Random;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Code by @author Wonsun Ahn
 * 
 * <p>Uses the Java Path Finder model checking tool to check BeanCounterLogic in
 * various modes of operation. It checks BeanCounterLogic in both "luck" and
 * "skill" modes for various numbers of slots and beans. It also goes down all
 * the possible random path taken by the beans during operation.
 */

public class BeanCounterLogicTest {
	private static BeanCounterLogic logic; // The core logic of the program
	private static Bean[] beans; // The beans in the machine
	private static String failString; // A descriptive fail string for assertions

	private static int slotCount; // The number of slots in the machine we want to test
	private static int beanCount; // The number of beans in the machine we want to test
	private static boolean isLuck; // Whether the machine we want to test is in "luck" or "skill" mode

	/**
	 * Sets up the test fixture.
	 */
	@BeforeClass
	public static void setUp() {
		if (Config.getTestType() == TestType.JUNIT) {
			slotCount = 5;
			beanCount = 3;
			isLuck = true;
		} else if (Config.getTestType() == TestType.JPF_ON_JUNIT) {
			/*
			 * TODO: Use the Java Path Finder Verify API to generate choices for slotCount,
			 * beanCount, and isLuck: slotCount should take values 1-5, beanCount should
			 * take values 0-3, and isLucky should be either true or false. For reference on
			 * how to use the Verify API, look at:
			 * https://github.com/javapathfinder/jpf-core/wiki/Verify-API-of-JPF
			 */
			slotCount = Verify.getInt(1, 5);
			beanCount = Verify.getInt(0, 3);
			isLuck = Verify.getBoolean();
		} else {
			assert (false);
		}

		// Create the internal logic
		logic = BeanCounterLogic.createInstance(slotCount);
		// Create the beans
		beans = new Bean[beanCount];
		for (int i = 0; i < beanCount; i++) {
			beans[i] = Bean.createInstance(slotCount, isLuck, new Random(42));
		}

		// A failstring useful to pass to assertions to get a more descriptive error.
		failString = "Failure in (slotCount=" + slotCount
				+ ", beanCount=" + beanCount + ", isLucky=" + isLuck + "):";
	}

	@AfterClass
	public static void tearDown() {
	}

	/**
	 * Test case for void void reset(Bean[] beans).
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 * Invariants: If beanCount is greater than 0,
	 *             remaining bean count is beanCount - 1
	 *             in-flight bean count is 1 (the bean initially at the top)
	 *             in-slot bean count is 0.
	 *             If beanCount is 0,
	 *             remaining bean count is 0
	 *             in-flight bean count is 0
	 *             in-slot bean count is 0.
	 */
	@Test
	public void testReset() {
		logic.reset(beans);
		int inFlightCount = 0;
		int inSlotCount = 0;
		if (beanCount > 0) {
			Assert.assertEquals(failString, beanCount - 1, logic.getRemainingBeanCount());
			// if slotCount == 1 then don't use the for loop. in for loop condition, 1-1 = 0 so the
			// in-flight bean will never be checked and the assertion will always fail
			// so just do it manually here.
			if (slotCount == 1) {
				if (logic.getInFlightBeanXPos(0) >= 0) {
					inFlightCount++;
				}
			} else {
				for (int y = 0; y < slotCount - 1; y++) {
					if (logic.getInFlightBeanXPos(y) >= 0) {
						inFlightCount++;
					}
				}
			}
			Assert.assertEquals(failString, 1, inFlightCount);
			
			for (int i = 0; i < slotCount; i++) {
				inSlotCount += logic.getSlotBeanCount(i);
			}
			Assert.assertEquals(failString, 0, inSlotCount);
		} else if (beanCount == 0) {
			Assert.assertEquals(failString, 0, logic.getRemainingBeanCount());
			
			if (slotCount == 1) {
				if (logic.getInFlightBeanXPos(0) >= 0) {
					inFlightCount++;
				}
			} else {
				for (int y = 0; y < slotCount - 1; y++) {
					if (logic.getInFlightBeanXPos(y) >= 0) {
						inFlightCount++;
					}
				}
			}
			Assert.assertEquals(failString, 0, inFlightCount);
			
			for (int i = 0; i < slotCount; i++) {
				inSlotCount += logic.getSlotBeanCount(i);
			}
			Assert.assertEquals(failString, 0, inSlotCount);
		}
	}

	/**
	 * Test case for boolean advanceStep().
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 * Invariants: After each advanceStep(),
	 *             all positions of in-flight beans are legal positions in the logical coordinate system.
	 */
	@Test
	public void testAdvanceStepCoordinates() {
		boolean stepSuccessful = true;
		boolean res;
		logic.reset(beans);
		do {
			stepSuccessful = logic.advanceStep();
			for (Bean b : beans) {
				res = false;
				if (b.getXPos() >= 0 && b.getXPos() < slotCount) {
					res = true;
				}
				Assert.assertTrue(failString, res);
			}
		} while (stepSuccessful);
	}

	/**
	 * Test case for boolean advanceStep().
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 * Invariants: After each advanceStep(),
	 *             the sum of remaining, in-flight, and in-slot beans is equal to beanCount.
	 */
	@Test
	public void testAdvanceStepBeanCount() {
		// System.out.println(failString);
		boolean stepSuccessful = true;
		int sum;
		int slotBeanCount;
		logic.reset(beans);
		do {
			sum = 0;
			stepSuccessful = logic.advanceStep();
			// number of beans still remaining
			sum += logic.getRemainingBeanCount();
			
			// number of beans in-flight
			for (int y = 0; y < slotCount; y++) {
				if (logic.getInFlightBeanXPos(y) != -1) {
					sum++;
				}
			}

			// number of beans in-slot
			for (int i = 0; i < slotCount; i++) {
				slotBeanCount = logic.getSlotBeanCount(i);
				if (slotBeanCount != -1) {
					sum += slotBeanCount;
				}
			}
			Assert.assertEquals(failString, beanCount, sum);
		} while (stepSuccessful);
	}

	/**
	 * Test case for boolean advanceStep().
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 * Invariants: After the machine terminates,
	 *             remaining bean count is 0
	 *             in-flight bean count is 0
	 *             in-slot bean count is beanCount.
	 */
	@Test
	public void testAdvanceStepPostCondition() {
		boolean stepSuccessful = true;
		int inFlight = 0;
		int inSlot = 0;
		logic.reset(beans);
		do {
			stepSuccessful = logic.advanceStep();
		} while (stepSuccessful);
		
		Assert.assertEquals(failString, 0, logic.getRemainingBeanCount());
		
		for (int y = 0; y < slotCount; y++) {
			if (logic.getInFlightBeanXPos(y) != -1) {
				inFlight++;
			}
		}
		Assert.assertEquals(failString, 0, inFlight);
		
		for (int i = 0; i < slotCount; i++) {
			inSlot += logic.getSlotBeanCount(i);
		}
		Assert.assertEquals(failString, beanCount, inSlot);
	}
	
	/**
	 * Test case for void lowerHalf()().
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 *                  Call logic.lowerHalf().
	 * Invariants: After the machine terminates,
	 *             remaining bean count is 0
	 *             in-flight bean count is 0
	 *             in-slot bean count is beanCount.
	 *             After calling logic.lowerHalf(),
	 *             slots in the machine contain only the lower half of the original beans.
	 *             Remember, if there were an odd number of beans, (N+1)/2 beans should remain.
	 *             Check each slot for the expected number of beans after having called logic.lowerHalf().
	 */
	@Test
	public void testLowerHalf() {
		logic.reset(beans);
		boolean stepSuccessful = true;
		int inFlight = 0;
		int inSlot = 0;
		int count;
		int[] beforeLowerHalf = new int[slotCount];
		int[] afterLowerHalf = new int[slotCount];
		for (int i = 0; i < slotCount; i++) {
			beforeLowerHalf[i] = 0;
			afterLowerHalf[i] = 0;
		}
		
		do {
			stepSuccessful = logic.advanceStep();
		} while (stepSuccessful);
		
		Assert.assertEquals(failString, 0, logic.getRemainingBeanCount());
		for (int y = 0; y < slotCount; y++) {
			if (logic.getInFlightBeanXPos(y) != -1) {
				inFlight++;
			}
		}
		Assert.assertEquals(failString, 0, inFlight);
		
		for (int i = 0; i < slotCount; i++) {
			count = logic.getSlotBeanCount(i);
			inSlot += count;
			beforeLowerHalf[i] = count;
		}
		Assert.assertEquals(failString, beanCount, inSlot);
		
		// calculate expected array
		int halfToKeep = 0;
		
		if (inSlot % 2 == 0) {
			halfToKeep = inSlot / 2; 
		} else {
			halfToKeep = (inSlot + 1) / 2; 
		}
		
		for (int i = 0; i < slotCount; i++) {
			count = logic.getSlotBeanCount(i);
			if (halfToKeep >= count) {
				beforeLowerHalf[i] = count;
				halfToKeep -= count;
				// have we officially kept all the beans we're going to?
			} else {
				beforeLowerHalf[i] = halfToKeep;
				break;
			}
		}
		logic.lowerHalf();
		
		// calculate actual
		for (int i = 0; i < slotCount; i++) {
			afterLowerHalf[i] = logic.getSlotBeanCount(i);
		}
		Assert.assertArrayEquals(failString, beforeLowerHalf, afterLowerHalf);
	}
	
	/**
	 * Test case for void upperHalf().
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 *                  Call logic.upperHalf().
	 * Invariants: After the machine terminates,
	 *             remaining bean count is 0
	 *             in-flight bean count is 0
	 *             in-slot bean count is beanCount.
	 *             After calling logic.upperHalf(),
	 *             slots in the machine contain only the upper half of the original beans.
	 *             Remember, if there were an odd number of beans, (N+1)/2 beans should remain.
	 *             Check each slot for the expected number of beans after having called logic.upperHalf().
	 */
	@Test
	public void testUpperHalf() {
		logic.reset(beans);
		boolean stepSuccessful = true;
		int inFlight = 0;
		int inSlot = 0;
		int count;
		int[] beforeUpperHalf = new int[slotCount];
		int[] afterUpperHalf = new int[slotCount];
		for (int i = 0; i < slotCount; i++) {
			beforeUpperHalf[i] = 0;
			afterUpperHalf[i] = 0;
		}
		
		do {
			stepSuccessful = logic.advanceStep();
		} while (stepSuccessful);
		
		Assert.assertEquals(failString, 0, logic.getRemainingBeanCount());
		for (int y = 0; y < slotCount; y++) {
			if (logic.getInFlightBeanXPos(y) != -1) {
				inFlight++;
			}
		}
		Assert.assertEquals(failString, 0, inFlight);
		
		for (int i = 0; i < slotCount; i++) {
			count = logic.getSlotBeanCount(i);
			inSlot += count;
			beforeUpperHalf[i] = count;
		}
		Assert.assertEquals(failString, beanCount, inSlot);
		
		// calculate expected array
		int halfToKeep = 0;
		
		if (inSlot % 2 == 0) {
			halfToKeep = inSlot / 2; 
		} else {
			halfToKeep = (inSlot + 1) / 2; 
		}
		
		for (int i = slotCount - 1; i >= 0; i--) {
			count = logic.getSlotBeanCount(i);
			if (halfToKeep >= count) {
				beforeUpperHalf[i] = count;
				halfToKeep -= count;
			} else {
				beforeUpperHalf[i] = halfToKeep;
				break;
			}
		}
		logic.upperHalf();
		
		// calculate actual
		for (int i = 0; i < slotCount; i++) {
			afterUpperHalf[i] = logic.getSlotBeanCount(i);
		}
		Assert.assertArrayEquals(failString, beforeUpperHalf, afterUpperHalf);
	}
	
	/**
	 * Test case for void repeat().
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 *                  Call logic.repeat();
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 * Invariants: If the machine is operating in skill mode,
	 *             bean count in each slot is identical after the first run and second run of the machine. 
	 */
	@Test
	public void testRepeat() {
		// if skill
		if (!isLuck) {
			logic.reset(beans);
			int[] firstPass = new int[slotCount];
			int[] secondPass = new int[slotCount];
			boolean stepSuccessful = true;
			do {
				stepSuccessful = logic.advanceStep();
			} while (stepSuccessful);
			for (int i = 0; i < slotCount; i++) {
				int count = logic.getSlotBeanCount(i);
				firstPass[i] = count;
			}
			logic.repeat();
			do {
				stepSuccessful = logic.advanceStep();
			} while (stepSuccessful);
			for (int i = 0; i < slotCount; i++) {
				int count = logic.getSlotBeanCount(i);
				secondPass[i] = count;
			}
			Assert.assertArrayEquals(failString, firstPass, secondPass);
		} else {
			logic.reset(beans);
			boolean stepSuccessful;
			do {
				stepSuccessful = logic.advanceStep();
			} while (stepSuccessful);
			
			logic.repeat();
			do {
				stepSuccessful = logic.advanceStep();
			} while (stepSuccessful);
		}
	}
	
	/**
	 * Test case for boolean getAverageSlotBeanCount().
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 *       			Call logic.getAverageSlotBeanCount();
	 *                  Call logic.repeat();
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 * Invariants: If the machine is operating in skill mode,
	 *             average slot bean count is identical after the first run and second run of the machine.
	 */
	@Test
	public void testAvgSlotBeanCount() {
		System.out.println("start");
		logic.reset(beans);
		double avg1, avg2;
		boolean stepSuccessful;
		do {
			stepSuccessful = logic.advanceStep();
		} while (stepSuccessful);
		avg1 = logic.getAverageSlotBeanCount();
		
		logic.repeat();
		do {
			stepSuccessful = logic.advanceStep();
		} while (stepSuccessful);
		avg2 = logic.getAverageSlotBeanCount();
		Assert.assertEquals(failString, avg1, avg2, 0.001);
	}
}
