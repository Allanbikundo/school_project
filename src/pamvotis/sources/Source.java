package src.pamvotis.sources;

import java.util.Vector;

import src.pamvotis.core.VirtualPacket;


/**
 * This abstract class represents a source that generates packets. Objects of this class should not created.
 * Create objects of the inherited class, that represent the type of source. This class is used by the simulation
 * engine to add packets to a node's queue. The abstract classes getnextPacket and synchronize should be implemented
 * from the inherited sources.
 * @author Dimitris El. Vassis
 */
public abstract class Source{

	/**
	 * The time slot value according to the physical layer. Used for synchronization
	 */
	public static float slot;
	/**
	 * Generator for generating random variables
	 */
	public static java.util.Random generator;
	
	/**
	 * The Id of the source
	 */
	public int _sourceId;
	/**
	 * Represents the current time in slots. used for synchronization
	 */
	public static long timer;
	/**
	 * The interarrival time of the packet to be generated.
	 */
	public int interArTime;
	/**
	 * The packet length of the packet to be generated.
	 */
	public int pktLength;
	/**
	 * The Id of the packet to be generated.
	 */
	public int packetToBeTransmittedID;
	/**
	 * A vector of virtual packets representing a session of packets. After generating a session of virtual packets,
	 * each one of them, when the time to be generated arrives, it is converted to a real packet and added to the source for transmission.
	 */
	public Vector<VirtualPacket> session = new Vector<VirtualPacket>();

	/**
	 * Generates a random variable obeying a specific distribution.
	 * @param distr The distribution which the random variable will obey: 'c' for constant, 'e' for exponential,
	 * 'p' for Pareto, 'l' for lognormal
	 * @param mean Mean value of the distribution
	 * @param stDev Standard deviation of the distribution
	 * @param max Maximum value. If this value is set, truncation will occur using the accept/reject method and the variable will obviously obey
	 * a truncated distribution.
	 * @param min Minimum value. If this value is set, truncation will occur using the accept/reject method and the variable will obviously obey
	 * a truncated distribution.
	 * @return A float which obeys the distribution set by 'dist'
	 */
	private float generateRandomVariable(char distr, float mean, float stDev, float max, float min){
		float rnd = 0;
		switch (distr) {
		case 'c': {
			rnd = (int) mean; //Casted because in most cases this value must be integer
			break;
		}
		case 'u': {
			do{
			rnd = mean*generator.nextFloat();
			}while((rnd>max)||(rnd<min));
			break;
		}
		case 'e': // exponential
		{
			do{
			rnd = - (float) (mean * Math.log(generator.nextDouble()));
			}while((rnd>max)||(rnd<min));
			break;
		}
		case 'l': // lognormal
		{
			double R1 = 0, R2 = 0, rNormal = 0, rLogNormal = 0, ml = 0, sl = 0;
			// Generate uniform
			do {
				// transform lognormal parameters to normal parameters
				ml = 2
				* java.lang.Math.log((double) mean)
				- java.lang.Math.log(java.lang.Math.pow((double) stDev,
						2)
						+ java.lang.Math.pow((double) mean, 2)) / 2;
				sl = java.lang.Math.sqrt(-2
						* java.lang.Math.log((double) mean)
						+ java.lang.Math.log(java.lang.Math.pow((double) stDev,
								2)
								+ java.lang.Math.pow((double) mean, 2)));
				// generate normal
				R1 = generator.nextDouble();
				R2 = generator.nextDouble();
				rNormal = ml + sl * java.lang.Math.cos(2 * 3.14 * R1)
				* java.lang.Math.sqrt(-java.lang.Math.log(R2));
				// generate lognormal
				rLogNormal = java.lang.Math.exp(rNormal);
			} while ((rLogNormal >= max)||(rLogNormal<=min)); // truncate
			rnd = (float)rLogNormal;
			break;
		}
		case 'p': //Pareto
		{
			double alfa = 1.2d; //alfa parameter of pareto is 1.2 for the video case. Is good in other telecom
			//stochastic cases as well.
			double b = 20*8;//(double)mean*(alfa-1d)/alfa;
			double d=0;
				d = generator.nextDouble();
				//generate a pareto distributed number 
				d = b/Math.pow(d,1d/alfa);
			rnd = (float) (d+b);//System.out.println(rnd);
			break;
		}
		default:
		{
			System.out.println("Distribution type configuration error. Use one of c,e,l or p.");
			System.exit(0);
		}
		}
		return rnd;
	}
	
	/**
	 * Generates a random variable obeying a specific distribution.
	 * @param distr The distribution which the random variable will obey: 'c' for constant, 'e' for exponential,
	 * 'p' for Pareto, 'l' for lognormal. For Pareto, alfa=1.2 is always assumed.
	 * @param mean Mean value of the distribution
	 * @param stDev Standard deviation of the distribution (only available for some distributions)
	 * @param max Maximum value. If this value is set, truncation will occur using the accept/reject method and the variable will obviously obey
	 * a truncated distribution. If you want to ommit this parameter, set it to Integer.MAX_VALUE
	 * @param min Minimum value. If this value is set, truncation will occur using the accept/reject method and the variable will obviously obey
	 * a truncated distribution. If you want to ommit this parameter, set it to -1.
	 * @return A float which obeys the distribution set by 'dist'
	 */
	public float genRv(char distr, float mean, float stDev, float max, float min){
			return generateRandomVariable(distr, mean, stDev, max, min);
	}
	
	/**
	 * Generates a random variable obeying a specific distribution. Should only be used for constant or exponential distribution.
	 * If used for Pareto, alfa=1.2 alway assumed.
	 * @param distr The distribution which the random variable will obey: 'c' for constant, 'e' for exponential,
	 * 'p' for Pareto.
	 * @param mean Mean value of the distribution
	 * @return A float which obeys the distribution set by 'dist'
	 */
	public float genRv(char distr, float mean){
			return generateRandomVariable(distr, mean, 0, Integer.MAX_VALUE, -1);
	}
	
	/**
	 * Generates a random variable obeying a specific distribution.
	 * @param distr The distribution which the random variable will obey: 'c' for constant, 'e' for exponential,
	 * 'p' for Pareto, 'l' for lognormal. For Pareto, alfa=1.2 is always assumed.
	 * @param mean Mean value of the distribution
	 * @param stDev Standard deviation of the distribution (only available for some distributions)
	 * @return A float which obeys the distribution set by 'dist'
	 */
	public float genRv(char distr, float mean, float stDev){
			return generateRandomVariable(distr, mean, stDev, Integer.MAX_VALUE, -1);
	}
	
	/**
	 * Generates a random variable obeying a specific distribution.
	 * @param distr The distribution which the random variable will obey: 'c' for constant, 'e' for exponential,
	 * 'p' for Pareto, 'l' for lognormal. For Pareto, alfa=1.2 is always assumed.
	 * @param mean Mean value of the distribution
	 * @param max Maximum value. If this value is set, truncation will occur using the accept/reject method and the variable will obviously obey
	 * a truncated distribution.
	 * @param min Minimum value. If this value is set, truncation will occur using the accept/reject method and the variable will obviously obey
	 * a truncated distribution.
	 * @return A float which obeys the distribution set by 'dist'
	 */
	public float genRv(char distr, float mean, float max, float min){
			return generateRandomVariable(distr, mean, 0, max, min);
	}
	
	/**This abstract method is called by the source manager to get a packet generated by the packet source.
	*Implemented differently in each source.
	*/
	public abstract void getNextPacket();
	
	/**This method is called by the simulator in every slot, to make synchronization procedures with the sources.
	*Mainly needed for the video source, but can be used by every source as a help function that is called in
	*every time slot.
	*/
	public abstract void synchronize();

}
