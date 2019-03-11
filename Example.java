import src.pamvotis.core.Simulator;
import src.pamvotis.exceptions.ElementDoesNotExistException;
import src.pamvotis.exceptions.ElementExistsException;
import src.pamvotis.exceptions.UnknownDistributionException;
import src.pamvotis.sources.FTPSource;
import src.pamvotis.sources.GenericSource;

//This class is just an example of how to use Pamvotis as an embedded simulator.
public class Example {

	public static void main(String[] args) {

		try {
			System.out.println("Simulation Started!");
			// First we must create a simulator object.
			Simulator sim = new Simulator();
			// Then we read the scenario parameters.
			sim.confParams();
			// We print the headers of the results files.
			sim.printHeaders();
			//We simulate for 10sec.
			sim.simulate(1, 10000);
			//We get the system throughput.
			System.out.println("Time: " + sim.getTime()+"sec"
					+ "\tSystem throughput: " + sim.getSysThrBps()+"Kbps");
			//We add a new node, with ID 3, being in position (17,17), with coverage 17m, and AC 0.
			sim.addNode(3, 1000000, 17, 17, 17, 0);
			//We add a generic source with ID 1 to node 3.
			sim.appendNewSource(3, new GenericSource(5, 'c', 8, 'c',8000));
			//We simulate for another 10sec
			sim.simulate(10001, 20000);
			//We get the system throughput and the media access delay of node 3.
			System.out.println("Time: " + sim.getTime()+"sec"
					+ "\tSystem throughput: " + sim.getSysThrBps()+"Kbps"
					+ "\tNode 3 media access delay: "
					+ sim.getMDelay(3)+"msec");
			//We add a new FTP source with ID 3, to node 3. Pay attention to 'f' after each float parameter. Needed for converting the integer to float.
			FTPSource ftps = new FTPSource(3, 8000, 1000000f, 300000f,3000000f, 50);
			sim.appendNewSource(3, ftps);
			//We simulate for another 10sec.
			sim.simulate(20001, 30000);
			//We get the system throughput and the media access delay of node 3.
			System.out.println("Time: " + sim.getTime()+"sec"
					+ "\tSystem throughput: " + sim.getSysThrBps()+"Kbps"
					+ "\tNode 3 media access delay: "
					+ sim.getMDelay(3)+"msec");
			//We remove the FTP source.
			sim.removeSource(3, 3);
			//We simulate for another 10sec.
			sim.simulate(30001, 40000);
			//We get the system throughput and the media access delay of node 3.
			System.out.println("Time: " + sim.getTime()+"sec"
					+ "\tSystem throughput: " + sim.getSysThrBps()+"Kbps"
					+ "\tNode 3 media access delay: "
					+ sim.getMDelay(3)+"msec");
			//We remove all sources of node 3.
			sim.removeAllSources(3);
			//We remove node 3. We could directly remove node 3 without first removing its sources.
			sim.removeNode(3);
			//We simulate for another 10sec.
			sim.simulate(40001, 50000);
			//We get the system throughput.
			System.out.println("Time: " + sim.getTime()+"sec"
					+ "\tSystem throughput: " + sim.getSysThrBps()+"Kbps");
			//We remove all nodes.
			sim.removeAllNodes();
			//We simulate for another 10sec.
			sim.simulate(50001, 60000);
			//We get the system throughput.
			System.out.println("Time: " + sim.getTime()+"sec"
					+ "\tSystem throughput: " + sim.getSysThrBps()+"Kbps");
			// We print the mean values for 0..60sec.
			sim.printMeanValues();
			System.out.println("Simulation finished!");
		} catch (ElementExistsException e) {
			e.printStackTrace();
		} catch (ElementDoesNotExistException e) {
			e.printStackTrace();
		} catch (UnknownDistributionException e) {
			e.printStackTrace();
		}
	}
}
