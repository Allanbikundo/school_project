package src.pamvotis.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import src.pamvotis.exceptions.ConfigurationException;
import src.pamvotis.exceptions.ElementDoesNotExistException;
import src.pamvotis.exceptions.ElementExistsException;
import src.pamvotis.exceptions.UnknownDistributionException;
import src.pamvotis.sources.FTPSource;
import src.pamvotis.sources.GenericSource;
import src.pamvotis.sources.HTTPSource;
import src.pamvotis.sources.Source;
import src.pamvotis.sources.VideoSource;
import src.pamvotis.core.utils.Generic;

/**
 * This is the basic class of Pamvotis that does all the work. It contains useful methods that the user can call
 * in order to configure a simulation, run a simulation splitted in time intervals and collect results for each
 * time interval or globally, for all the simulation.
 * @author Dimitris El. Vassis
 * @Revision notes
 * Added reference to new package pamvotis.core.utils
 */
public class Simulator {

    /**
     * Creates a new simulation instance.
     * When a new instance is created, the parameters of the 802.11 standard family are read.
     * @see SpecParams#ReadParameters()
     */
    public Simulator() {
        SpecParams.ReadParameters();
        //Although all members of SpecParams are static, the ReadParameters function must be called once, in order
        //to read the xml file and initialize the members.
    }
    //Global parameters are defined here, visible to all methods of the class.
    private int seed = 0;	//Stores the seed from the xml file.
    private long totalTime = 0;	//Stores the total time (not the duration of each simulation interval.
    //It is read from the xml file.
    private long simTime = 0;	//Stores the current simulation interval.
    private double currentTime = 0;	//Stores the current time in seconds. Used for the interface
    //or the program that calls an instance of Simulator class.
    private int nmbrOfNodes = 0;	//The number of nodes (read from the xml file).
    private int mixNodes = 0;	//The number of 802.11b compliant nodes (only in mixed 802.11b/g).
    private int rtsThr = 0;	//The RTS threshold (read from the xml file).
    private char ctsToSelf = 'n';	//Determines if CTSToSelf is used (read from the xml file).
    private char phyLayer = 's';	//The physical layer (read from the xml file) default simple.
    private int cwMin;	//The mimimum contention window for the physical layer.
    //It is defined according to the physical layer. It has nothing to do with the cWmin of each node.
    //Each node defines it's cwMin according to the this variable and the parameters of 802.11e.
    private float sifs;	//The sifs value according to the physical layer.
    private float slot;	//The time slot value according to the physical layer.
    private String resultsPath = null;	//The path to store the result files (read from the xml file).
    private String outResults = null;	//A string that defines which results the user chose.
    private short progress = 0;	//The percentage of the progress. Used for the progress bar.
    private BufferedWriter out = null; //writer for the results files
    private java.util.Random generator;	//An instance for generating random numbers.
    //Helpful global variables
    private boolean transmissionPending = false; //checks if a station is transmitting.
    private int transTimeRemaining = 0; //The time for a transmission to finish.
    private boolean transmitWithRTS = false;	//flag which shows if a station transmits with RTS/CTS.
    private static final int INT_MAX = 999999999;
    //The vector  that holds the nodes
    private Vector<MobileNode> nodesList = new Vector<MobileNode>();
    //802.11 parameters, needed for creating nodes:
    private int cwMinFact0 = 0;
    private int cwMinFact1 = 0;
    private int cwMinFact2 = 0;
    private int cwMinFact3 = 0;
    private int cwMaxFact0 = 0;
    private int cwMaxFact1 = 0;
    private int cwMaxFact2 = 0;
    private int cwMaxFact3 = 0;
    private int aifs0 = 0;
    private int aifs1 = 0;
    private int aifs2 = 0;
    private int aifs3 = 0;
//  private static int colls = 0;
    private int currentStrategy = -1;
    private long collisionsForAllNodes = 0;//in a particular simulation interval
    private long successForAllNodes = 0;

    //*****************		FUNCTIONS FOLLOW		************************************
    ////////////////////////////////////////////////////////////////
    //	 *****			FightForSlot			*****
    //Depending on the value of transmissionPending
    //check if a slot is empty,or there is a successfulTransmission
    //or collision. In either case execute the corresponding routine.
    /////////////////////////////////////////////////////////////////
    private void fightForSlot() {

        int distance = INT_MAX; //Help variable for checking the LOS.
        int transRequests = 0; //The number of nodes that want to transmit.
        int transNlos = 0;	//The number of stations that want to transmit
        // and are not in LOS with the transmitting stations.
        int coverage = INT_MAX;	//Helpful variable.

        //For each station except the transmitting one check if it has
        //a packet to send and if the backoff counter expired.
        //If so, set the 'requestTransmit' flag to enabled.
        for (int i = 0; i < nmbrOfNodes; i++) {


             //add debug
             //Abiud/AYIENGA added nodeId to track status of each and every node.
            // Also added the debug logs
           int nodeId  = nodesList.elementAt(i).params.id;

          //System.out.println("Transmission Status for node [" + nodeId + "]  collision block is : " + nodesList.elementAt(i).successfullyTransmitting);
          //System.out.println("Number of successful transmissions for for node [" + nodeId + "] " +  nodesList.elementAt(i).successfulTransmissions);
         // System.out.println("Does node with id  [" + nodeId + "] have packets to send ?" +  nodesList.elementAt(i).havePktToSend);
         // System.out.println("Number of collisions node  [" + nodeId + "] has encountered are : " +  nodesList.elementAt(i).collisions);


            if ((nodesList.elementAt(i).nowTransmitting == false) && (nodesList.elementAt(i).havePktToSend == true)
                    && (nodesList.elementAt(i).backoffCounter == 0)) {
                nodesList.elementAt(i).requestTransmit = true;
            }

            //Depending on the value of 'requestTransmit' find the
            //number of stations that want to transmit in this slot.
            transRequests = transRequests + ((nodesList.elementAt(i).requestTransmit) ? 1 : 0); //The last expression transforms the requestTransmit value to boolean
        }

        //If a transmission is in progress (successful or collision):
        if (transmissionPending == true) {
            //If the station transmits with RTS/CTS, then all other stations
            //can hear  the transmission and must refain (freeze).
            //No hidden terminals exist. The TransWithRTS flag is initialized
            //by the SuccessfulTransmission procedure.
            if (transmitWithRTS == true) {
                freeze();
            } //If CTS-to_Self or simple transmission is used and
            //a transmission is in progress:
            else {

                //Find the number of stations that want to transmit and are not
                //in LOS with any of the transmitting stations.
                for (int i = 0; i < nmbrOfNodes; i++) {
                    distance = INT_MAX;

                    //Find the minimum distance from station i to the transmitting
                    // stations.
                    for (int j = 0; j < nmbrOfNodes; j++) {
                        if ((nodesList.elementAt(j).nowTransmitting == true)
                                && (distance > nodesList.elementAt(i).params.DistFrom(nodesList.elementAt(j).params.x,
                                nodesList.elementAt(j).params.y))) {
                            distance = nodesList.elementAt(i).params.DistFrom(nodesList.elementAt(j).params.x,
                                    nodesList.elementAt(j).params.y);
                        }
                    }

                    //If the minimum distance is more than the coverage of the
                    //station and if the station wants to transmit increase the number
                    //of stations that want to transmit and are not in LOS
                    //with any of the transmitting stations.
                    coverage = nodesList.elementAt(i).params.coverage;
                    if ((distance > coverage)
                            && (nodesList.elementAt(i).requestTransmit == true)) {
                        transNlos++;
                    } //If the station is not transmitting, is not in line of sight
                    //and is in backoff procedure, decrease its backoff counter.
                    else if ((nodesList.elementAt(i).backoffCounter > 0)
                            && (distance > coverage)
                            && (nodesList.elementAt(i).nowTransmitting == false)) {
                        nodesList.elementAt(i).backoffCounter--;
                    }
                }

                //If there are hidden terminals that want to transmit then
                //a collision occurs.
                if (transNlos > 0) {
                    collision();
                } //If no station wants to transmit or those who want to transmit
                //are in LOS with some transmitting station then just Freeze.
                else {
                    freeze();
                }
            }
        } //if no transmission is in progress:
        else {
            //If no station wants to transmit then the slot is empty.
            if (transRequests == 0) {
                emptySlot();
            } //If some stations want to transmit:
            else {
                //If only one station wants to transmit there is a successful
                //transmission. The transWithRTS flag shows if the transmission
                //is performed with RTS/CTS. This is used from the FightForSlot
                //procedure.
                if (transRequests == 1) {
                    transmitWithRTS = successfulTransmission();
                } //If more than one station wants to transmit there is a collision.
                else {
                    collision();

                }

                //In either a successful transmission or collision find the
                //hidden terminals which are in backoff procedure and decrease
                //their backoff counter.
                //(Those stations cannot hear the transmission).
                for (int i = 0; i < nmbrOfNodes; i++) {
                    distance = INT_MAX;

                    //Find the minimum distance from node i to the
                    //transmitting node.
                    for (int j = 0; j < nmbrOfNodes; j++) {
                        if ((nodesList.elementAt(j).nowTransmitting == true)
                                && (distance > nodesList.elementAt(i).params.DistFrom(nodesList.elementAt(j).params.x, nodesList.elementAt(j).params.y))) {
                            distance =
                                    nodesList.elementAt(j).params.DistFrom(nodesList.elementAt(j).params.x,
                                    nodesList.elementAt(j).params.y);
                        }
                    }
                    //If the station is not transmitting, is not in line of sight
                    //and is in backoff procedure, decrease its backoff counter.
                    coverage = nodesList.elementAt(i).params.coverage;
                    if ((nodesList.elementAt(i).backoffCounter > 0) && (distance > coverage)
                            && (nodesList.elementAt(i).nowTransmitting == false)) {
                        nodesList.elementAt(i).backoffCounter--;
                    }
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////
    //	*****			EmptySlot			*****
    //Performs the necessary actions
    //if a slot is empty (no station wants to transmit).
    ////////////////////////////////////////////////////////////////
    private void emptySlot() {
        //Decrease the backoff counter of each station and set
        //the value of transmissionPending to false
        //(no station will transmit in the next slot).
        for (int i = 0; i < nmbrOfNodes; i++) {
            if (nodesList.elementAt(i).backoffCounter > 0) {
                nodesList.elementAt(i).backoffCounter--;
            }
        }
        transmissionPending = false;
    }

    /////////////////////////////////////////////////////////////
    //	*****			SuccessfulTransmission			*****
    //Performs the necessary actions
    //if only one station wants to trasmit.
    /////////////////////////////////////////////////////////////
    private boolean successfulTransmission() {
        int transNode = -1;	//The node which transmits.
        boolean transWithRTS = true;
        float probOFDM = 0;	//The probability an OFDM transmission to occur
        //in a mixed mode (b/g) network.
        char transType = 'O'; //The type of the transmission ('O' for OFDM
        //and 'D' for DSSS.
        int ACK = SpecParams.ACK, RTS = SpecParams.RTS, CTS = SpecParams.CTS, MAC = SpecParams.MAC;
        float OFDM_PHY = SpecParams.OFDM_PHY;

        for (int i = 0; i < nmbrOfNodes; i++) {
            //Find the node which wants to transmit.
            //Enable the flag 'successfullyTransmitting'. This will be used
            //by the freeze procedure to find the transmitting station.
            //Disable the request transmit flag.
            if (nodesList.elementAt(i).requestTransmit == true) {
                transNode = i;
                nodesList.elementAt(i).successfullyTransmitting = true;
                nodesList.elementAt(i).nowTransmitting = true;
                nodesList.elementAt(i).requestTransmit = false;
            }
        }

        //If the network is in mixed mode (existense of 802.11b stations
        //in an 802.11g network generate a transmission (DSSS or OFDM) according
        //to the number of 802.11b nodes.
        //First find the probability an FODM transmission to occur. This is the
        //probability the transmitting station to be 802.11g station, and the
        //receiving station to be 802.11g station, given that the transmitting
        //station is 802.11g station. This is:
        probOFDM = (float) (nmbrOfNodes - mixNodes) / (float) (nmbrOfNodes)
                * (float) (nmbrOfNodes - mixNodes - 1) / (nmbrOfNodes - 1);

        //Now perform a Bernoulli trial with probability of success probOFDM.
        //First generate a random number between 0 and 1. If the number is
        //<probOFDM then we have a success (OFDM transmission). Else we have
        //a failure (DSSS transmission).
        float rand01 = generator.nextFloat();
        if (phyLayer == 'm') {
            if (rand01 < probOFDM) {
                transType = 'O';
            } else {
                transType = 'D';
            }
        } else {
            transType = 'U';	//No mixed mode.
        }
        //Initialize the remaining time until transmission
        //(transTimeRemaining)

        //Take the packet payload and the data rate of the transmitting
        //node.
        int payld = nodesList.elementAt(transNode).pktLength;
        int rate = nodesList.elementAt(transNode).params.rate;

        //802.11a or 802.11g
        if ((phyLayer == 'a') || (phyLayer == 'g') || (transType == 'O')) {
            //transmission with protection mechanisms
            if (payld > rtsThr) {
                //Transmission with RTS/CTS
                if (ctsToSelf == 'n') {
                    transWithRTS = true;
                    transTimeRemaining = (int) ((nodesList.elementAt(transNode).params.aifsd + 3 * sifs + 4 * OFDM_PHY) / slot)
                            + (RTS + CTS + MAC + payld + ACK + padBits(rate, RTS)
                            + padBits(rate, CTS) + padBits(rate, MAC) + padBits(rate, payld)
                            + padBits(rate, ACK)) / (int) (rate * slot);
                } //Transmission with CTS-to-Self
                else {
                    transWithRTS = false;
                    transTimeRemaining = (int) ((nodesList.elementAt(transNode).params.aifsd + 2 * sifs + 3 * OFDM_PHY) / slot)
                            + (CTS + MAC + payld + ACK + padBits(rate, CTS) + padBits(rate, MAC)
                            + padBits(rate, payld) + padBits(rate, ACK)) / (int) (rate * slot);
                }

            } //Transmission with basic access
            else {
                transWithRTS = false;
                transTimeRemaining = (int) ((nodesList.elementAt(transNode).params.aifsd + sifs + 2 * OFDM_PHY) / slot)
                        + (MAC + payld + ACK + padBits(rate, MAC) + padBits(rate, payld)
                        + padBits(rate, ACK)) / (int) (rate * slot);
            }
        }

        //802.11b or mixed 802.11g / 802.11b
        if ((phyLayer == 'b') || (phyLayer == 's') || (transType == 'D')) {
            //Define the preamble type (short or long) depending on
            //the physical layer.
            float phy = (float) SpecParams.SHORT_PHY;
            if (phyLayer == 's') {
                phy = (float) SpecParams.LONG_PHY;
            }

            //transmission with protection mechanisms
            if (payld > rtsThr) {
                //Transmission with RTS/CTS
                if (ctsToSelf == 'n') {
                    transWithRTS = true;
                    transTimeRemaining = (int) ((nodesList.elementAt(transNode).params.aifsd + 3 * sifs + 4 * phy) / slot)
                            + (RTS + CTS + MAC + payld + ACK) / (int) (rate * slot);
                } //Transmission with CTS-to-Self
                else {
                    transWithRTS = false;
                    transTimeRemaining = (int) ((nodesList.elementAt(transNode).params.aifsd + 2 * sifs + 3 * phy) / slot)
                            + (CTS + MAC + payld + ACK) / (int) (rate * slot);
                }

            } //Transmission with basic access
            else {
                transWithRTS = false;
                transTimeRemaining = (int) ((nodesList.elementAt(transNode).params.aifsd + sifs + 2 * phy) / slot)
                        + (MAC + payld + ACK) / (int) (rate * slot);
            }
        }

        //Enable the 'transmissionPending' flag. This is used from the
        //nodes in the start of the main loop to see
        //if a transmission is in progress.
        transmissionPending = true;

        //Begin transmission
        freeze();
        return transWithRTS;
    }

    //////////////////////////////////////////////////////////////
    //	 *****						Collision			*****
    //	Performs the necessary actions if more than one nodes
    //	want to transmit.
    ////////////////////////////////////////////////////////////
    private void collision() {

        System.out.println("CHECKING FOR COLLIDING__________________________________________________________________");
        int maxPld = 0;	//The maximum payload under transmission duration
        int maxLsThr = 0;	//The maximum payload smaller than RTS threshold
        float maxTrans = 0; //The maximum transmission time.
        float maxLsTrans = 0;	//The maximum transmission duration for
        // packets smaller than the RTS threshold.
        int maxNode = -1;	//The node with the maximum transmission duration
        int maxLsNode = -1; //The node with the maximum transmission
        //duration whose packet is smaller than the RTS threshold

        int RTS = SpecParams.RTS, CTS = SpecParams.CTS, MAC = SpecParams.MAC, ACK = SpecParams.ACK;
        float OFDM_PHY = SpecParams.OFDM_PHY;


        int transTimeRemainingOld = 0; //buffer to store the current time remaining
        //for the end of the transmission (if there is one);
        boolean los = true; //flag for checking if a station is in line of sight
        //with the transmitting station.
        int rate = 0;	//Variable which stores  a node's data rate
        int payld = 0;	//Variable which stores a node's payload
        int distance = -1;	//Helpful variable.
        int coverage = INT_MAX; //Helpful variable.

        //Store the remaining time for the end of a transmission (if any) to a temporary
        //variable. If no transmission is in progress then this value is zero.
        transTimeRemainingOld = transTimeRemaining;


        //For each station that wants to transmit and is not in los with the
        //transmitting station  (if there is one) initialize the backoff counter
        //and calculate packet measures used for determining the collision duration.
        //The transmitting stations (if any) are excluded.
        for (int i = 0; i < nmbrOfNodes; i++) {
            los = true; //If no station transmits we want the 'los' flag to be true by default.
            distance = INT_MAX;
            //If a station transmits go to the next one.
            if (nodesList.elementAt(i).nowTransmitting == true) {
                continue;
            }

            //Find the minimum distance from node i to the transmitting stations.
            for (int j = 0; j < nmbrOfNodes; j++) {
                if ((nodesList.elementAt(j).nowTransmitting == true)
                        && (distance > nodesList.elementAt(i).params.DistFrom(nodesList.elementAt(j).params.x, nodesList.elementAt(j).params.y))) {
                    distance =
                            nodesList.elementAt(i).params.DistFrom(nodesList.elementAt(j).params.x,
                            nodesList.elementAt(j).params.y);
                }
            }

            //If the minimum distance is more than the coverage of the station or if
            //no stations transmit (distance remains INT_MAX) then the station is not in LOS with some
            //of the stations transmitting, and the los flag is false.
            coverage = nodesList.elementAt(i).params.coverage;
            if ((distance > coverage) || (distance == INT_MAX)) {
                los = false;
            } else {
                los = true;
            }

            //If a station wants to transmit and is in not in los with the transmitting
            //station (if any) then a collision occurs. If no station transmits then
            //the los flag is false by default which means that the station will transmit.
            if ((nodesList.elementAt(i).requestTransmit == true) && (los == false)) {
                //The startTransmitting flag is used instead of the nowTransmitting
                //in order the nowTransmitting flag to stay unaffected.
                nodesList.elementAt(i).startTransmitting = true;

                //Find the largest packet for transmission. This specifies
                //the collision time.
                if (nodesList.elementAt(i).pktLength >= maxPld) {
                    maxPld = nodesList.elementAt(i).pktLength;
                }

                //Find the largest packet that is less than the RTS Threshold.
                if (nodesList.elementAt(i).pktLength <= rtsThr) {
                    maxLsThr = nodesList.elementAt(i).pktLength;
                }

                //Find the station with the longest transmission
                // for packets smaller than the RTS threshold.
                if (((float) nodesList.elementAt(i).pktLength
                        / (float) nodesList.elementAt(i).params.rate > maxLsTrans)
                        && (nodesList.elementAt(i).pktLength <= rtsThr)) {
                    maxLsTrans = (float) nodesList.elementAt(i).pktLength
                            / (float) nodesList.elementAt(i).params.rate;
                    maxLsNode = i;
                }

                //Find the station with the longest transmission.
                if ((float) nodesList.elementAt(i).pktLength
                        / (float) nodesList.elementAt(i).params.rate > maxTrans) {
                    maxTrans = (float) nodesList.elementAt(i).pktLength
                            / (float) nodesList.elementAt(i).params.rate;
                    maxNode = i;
                }
                /*
                //Initialize the contention window of the station.
                if (nodesList.elementAt(i).contWind < nodesList.elementAt(i).params.cwMax) {
                nodesList.elementAt(i).contWind *= 2;
                } //We add an else statement because in extreme cases (the user may choose cwmin=cwmax)
                //cwmin may be bigger than cwmax if it gets double.
                else {
                nodesList.elementAt(i).contWind = nodesList.elementAt(i).params.cwMax;
                }*/
                //SIMON MODIFIED THIS CODE
                //_______________________________________________________________________________________________________________________________________________

                 System.out.println(" Initial value for Contwind was => " + nodesList.elementAt(i).contWind);

                if (getCurrentStrategy() == 1) {
                    //The existing DCF strategy
                    if (nodesList.elementAt(i).contWind < nodesList.elementAt(i).params.cwMax) {
                    	System.out.println("The Contention Window Min for 1 is:" + cwMin);
                        nodesList.elementAt(i).contWind = Generic.getRandomNumber(1, cwMin);
                        System.out.println("The Contention Window Using Strategy 1 is:" + nodesList.elementAt(i).contWind);
                        System.out.println("The Contention Window Max for 1 is:" + nodesList.elementAt(i).params.cwMax);
                    } else //We add an else statement because in extreme cases (the user may choose cwmin=cwmax)
                    //cwmin may be bigger than cwmax if it gets double.
                    {
                        nodesList.elementAt(i).contWind = nodesList.elementAt(i).params.cwMax;
                    }
                   // System.out.println("USING 1");
                } else if (getCurrentStrategy() == 2) {
                    /*A strategy that selects backoff values from a different distribution
                    with a smaller average backoff value, than the distribution specified by
                    DCF (e.g. by selecting backoff values from the range [0,  ] instead of [0, CW].
                     *
                     */
                    //if (nodesList.elementAt(i).contWind < nodesList.elementAt(i).params.cwMin) {   /* changed on 01-march-2014 4.18pm    */
               
                       if (nodesList.elementAt(i).contWind < nodesList.elementAt(i).params.cwMax) {
                        //cwMin = ((cwMin + 1)/2) - 1;
                        //cwMin = ((cwMin )/2);
                    	   
                    	nodesList.elementAt(i).contWind = Generic.getRandomNumber(1, (cwMin/2));
                    	 System.out.println("The Contention Window for Strategy 2 is:" + nodesList.elementAt(i).contWind);
                    } else {
                        nodesList.elementAt(i).contWind = nodesList.elementAt(i).params.cwMax;
                    }
                    //System.out.println("USING 3");
                } else if (getCurrentStrategy() == 3) {
                    //Selecting a fixed backoff of one slot.
                    if (nodesList.elementAt(i).contWind < nodesList.elementAt(i).params.cwMax) {  
                    	System.out.println("The Contention Window for 3 is:" + nodesList.elementAt(i).contWind);
                    	//cwMin = (cwMin + 1)/4 - 1;
                          //int upper  = ((cwMin )/2) - 1;
                          //int lower  = ((cwMin )/4) - 1;
                         //cwMin = ((cwMin )/2) - 1;

                          int upper  = ((cwMin )/2);
                          int lower  = ((cwMin )/4);
                        //cwMin  = Generic.getRandomNumber(lower, upper);
                    	nodesList.elementAt(i).contWind = Generic.getRandomNumber(lower, upper);
                    	System.out.println("The Contention Window Min for 3 is:" + cwMin);
                    	System.out.println("The Contention Window for Strategy 3 is:" + nodesList.elementAt(i).contWind);
                    	System.out.println("The Contention Window Max for 3 is:" + nodesList.elementAt(i).params.cwMax);
                    } else {
                        nodesList.elementAt(i).contWind = nodesList.elementAt(i).params.cwMax;
                    }
                   // System.out.println("USING 3a");
                } else if (getCurrentStrategy() == 4) {
                    //Selecting a fixed backoff of one slot.
                    if (nodesList.elementAt(i).contWind < nodesList.elementAt(i).params.cwMax) {
                        nodesList.elementAt(i).contWind = 8;
                        System.out.println("The Contention Window for 4 is:" + nodesList.elementAt(i).contWind);
                    } else {
                        nodesList.elementAt(i).contWind = nodesList.elementAt(i).params.cwMax;
                    }
                    System.out.println("USING 4a");
               }

                //END OF SIMON'S MODIFICATIONS

                //Initialize the backoff counter of the station.
                nodesList.elementAt(i).backoffCounter = nodesList.elementAt(i).InitBackoff(nodesList.elementAt(i).contWind);

                //Disable the request transmit flag.
                nodesList.elementAt(i).requestTransmit = false;

                //Increase the number of collisions.
                nodesList.elementAt(i).collisions++;
            }
        }

        //If some stations transmit with the basic access, find the payload and
        //the data rate for the maximum transmission duration.

        //No colliding station transmits with protection mechanisms enabled.
        if (maxPld < rtsThr) {
            //The duration of a collision is the maximum transmission
            //duration of colliding nodes.
            rate = nodesList.elementAt(maxNode).params.rate;
            payld = nodesList.elementAt(maxNode).pktLength;
        } //At least one station (but not all) transmits without
        //protection mechanisms.
        else if (maxLsThr != 0) {
            //The duration of a collision is the maximum transmission
            //duration of colliding nodes whose packets are smaller
            //than the RTS threshold.
            rate = nodesList.elementAt(maxLsNode).params.rate;
            payld = nodesList.elementAt(maxLsNode).pktLength;
        } //All stations transmit with protection mechanisms
        else {
            rate = nodesList.elementAt(maxNode).params.rate;
            payld = nodesList.elementAt(maxNode).pktLength;
        }

        //Initialize the transmission duration

        //802.11a or 802.11g
        if ((phyLayer == 'a') || (phyLayer == 'g')) {
            //At least one station transmits without protection mechanisms
            if (maxLsThr != 0) {

                transTimeRemaining = (int) ((nodesList.elementAt(maxLsNode).params.aifsd + OFDM_PHY) / slot)
                        + (MAC + payld + padBits(rate, MAC) + padBits(rate, payld))
                        / (int) (rate * slot);
            } //All stations transmit with protection mechanisms
            else {
                //Transmission with RTS/CTS
                if (ctsToSelf == 'n') {
                    //Tc = RTS+SIFS+ACK+DIFS, not RTS+DIFS. This is because of the use
                    //of EIFS, which means that all stations must wait until the ACK timeout reception is over,
                    //in order to use the medium.
                    //RTS/CTS is transmitted with the minimum data rate.
                    transTimeRemaining = (int) ((nodesList.elementAt(maxNode).params.aifsd + 2 * OFDM_PHY + sifs) / slot)
                            + (RTS + ACK + padBits(6000000, RTS) + padBits(6000000, ACK)) / (int) (6000000 * slot);
                } //Transmission with CTS-to-Self
                else {
                    transTimeRemaining = (int) ((nodesList.elementAt(maxNode).params.aifsd + OFDM_PHY) / slot)
                            + (CTS + padBits(6000000, CTS)) / (int) (6000000 * slot);
                }
            }
        } //802.11b or mixed 802.11g / 802.11b
        else //802.11, 802.11b or mixed mode. For mixed mode we assume that
        //at least one station transmits with DSSS, the transmission of
        //which is longer.
        {
            //Define the preamble type (short or long) depending on
            //the physical layer.
            float phy = (float) SpecParams.SHORT_PHY;
            if (phyLayer == 's') {
                phy = (float) SpecParams.LONG_PHY;
            }

            //At least one station transmits without protection mechanism
            if (maxLsThr != 0) {

                transTimeRemaining = (int) ((nodesList.elementAt(maxLsNode).params.aifsd + phy) / slot)
                        + (MAC + payld) / (int) (rate * slot);
            } //All stations transmit with protection mechanisms
            else {
                //Transmission with RTS/CTS
                if (ctsToSelf == 'n') {
                    //Tc = RTS+SIFS+ACK+DIFS, not RTS+DIFS. This is because of the use
                    //of EIFS, which means that all stations must wait until the ACK timeout reception is over,
                    //in order to use the medium.
                    //RTS/CTS is transmitted with the minimum data rate.
                    transTimeRemaining = (int) ((nodesList.elementAt(maxNode).params.aifsd + 2 * phy + sifs) / slot)
                            + (RTS + ACK) / (int) (1000000 * slot);
                } //Transmission with CTS-to-Self
                else {
                    transTimeRemaining = (int) ((nodesList.elementAt(maxNode).params.aifsd + phy) / slot)
                            + CTS / (int) (1000000 * slot);
                }
            }
        }

        //Initialize the backoff counter of the transmitting stations (if any).
        for (int i = 0; i < nmbrOfNodes; i++) {
           if(nodesList.elementAt(i).nowTransmitting==true)
			{
				//Initialize the contention window of the station.
				if(nodesList.elementAt(i).contWind < nodesList.elementAt(i).params.cwMax){
					nodesList.elementAt(i).contWind *= 2;

                                        System.out.println("The Contention Window Min for 1 is:" + cwMin);
                                        nodesList.elementAt(i).contWind *= 2;
                                        System.out.println("The Contention Window for 1 is:" + nodesList.elementAt(i).contWind);
                                        System.out.println("The Contention Window Max for 1 is:" + nodesList.elementAt(i).params.cwMax);

                            }

				else
					//We add an else statement because in extreme cases (the user may choose cwmin=cwmax)
					//cwmin may be bigger than cwmax if it gets double.
					nodesList.elementAt(i).contWind = nodesList.elementAt(i).params.cwMax;

				//Initialize the backoff counter of the station.
				nodesList.elementAt(i).backoffCounter = nodesList.elementAt(i).InitBackoff(nodesList.elementAt(i).contWind);

				//Disable the request transmit flag.
				nodesList.elementAt(i).requestTransmit = false;

				//Increase the number of collisions.
				nodesList.elementAt(i).collisions ++;

				//Disable the successfullyTransmitting flag of the station (if enabled).
				nodesList.elementAt(i).successfullyTransmitting = false;
			}



                //SIMON MODIFIED THIS CODE
                //_______________________________________________________________________________________________________________________________________________
            	/*if (getCurrentStrategy() == 1) {
                    //The existing DCF strategy
                    if (nodesList.elementAt(i).contWind < nodesList.elementAt(i).params.cwMax) {
                        nodesList.elementAt(i).contWind *= 2;
                    } else //We add an else statement because in extreme cases (the user may choose cwmin=cwmax)
                    //cwmin may be bigger than cwmax if it gets double.
                    {
                        nodesList.elementAt(i).contWind = nodesList.elementAt(i).params.cwMax;
                    }
                    System.out.println("Collision using 1");
                } else if (getCurrentStrategy() == 2) {
                    /*A strategy that selects backoff values from a different distribution
                    with a smaller average backoff value, than the distribution specified by
                    DCF (e.g. by selecting backoff values from the range [0,  ] instead of [0, CW].
                     *
                     
                    if (nodesList.elementAt(i).contWind < nodesList.elementAt(i).params.cwMin) {
                        cwMin = (cwMin + 1)/2 - 1;
                    	nodesList.elementAt(i).contWind *= 2;
                    } else {
                        nodesList.elementAt(i).contWind = nodesList.elementAt(i).params.cwMin;
                    }
                    System.out.println("Collision using 2");
                } else if (getCurrentStrategy() == 3) {
                    //Selecting a fixed backoff of one slot.
                    if (nodesList.elementAt(i).contWind < nodesList.elementAt(i).params.cwMin - 1/2) {
                    	cwMin = (cwMin + 1)/4 - 1;
                    	nodesList.elementAt(i).contWind *= 2;
                    } else {
                        nodesList.elementAt(i).contWind = nodesList.elementAt(i).params.cwMax;
                    }
                    System.out.println("Collision using 3");
                } else if (getCurrentStrategy() == 4) {
                    //Selecting a fixed backoff of one slot.
                    if (nodesList.elementAt(i).contWind < nodesList.elementAt(i).params.cwMax) {
                        nodesList.elementAt(i).contWind = 8;
                    } else {
                        nodesList.elementAt(i).contWind = nodesList.elementAt(i).params.cwMax;
                    }
                    System.out.println("Collision using 4");
               }
                //END OF SIMON'S MODIFICATIONS

                //Initialize the backoff counter of the station.
                nodesList.elementAt(i).backoffCounter = nodesList.elementAt(i).InitBackoff(nodesList.elementAt(i).contWind);

                //Disable the request transmit flag.
                nodesList.elementAt(i).requestTransmit = false;

                //Increase the number of collisions.
                nodesList.elementAt(i).collisions++;

                //Disable the successfullyTransmitting flag of the station (if enabled).
                nodesList.elementAt(i).successfullyTransmitting = false;
            }
            */


        }

        //Fix the nowTransmitting flag of each station.
        //The nowTransmitting flag is checked from outside procedures.
        //The startTransmitting flag is for internal use, for not affecting
        //the nowTransmitting flag.
        for (int i = 0; i < nmbrOfNodes; i++) {
            if (nodesList.elementAt(i).startTransmitting == true) {
                nodesList.elementAt(i).startTransmitting = false;
                nodesList.elementAt(i).nowTransmitting = true;
            }
        }

        //If the ramaining time for transmission is less than the new specified time
        //keep the old one.
        if (transTimeRemaining < transTimeRemainingOld) {
            transTimeRemaining = transTimeRemainingOld;
        }

        //Enable the 'transmissionPending' flag. This
        //is used from the stations in the start of the main loop to see
        //if a transmission is in progress.
        transmissionPending = true;

        //Begin transmission
        freeze();
    }

    //////////////////////////////////////////////////////////////////////
    //	*****			PrintStats			*****
    //Prints statistic results to text files.
    //Each statistic is printed only if the user asks it (depending on the
    //containing characteristic two-character sets of the reslts string).
    //////////////////////////////////////////////////////////////////////
    private void printStats() {

        float result = 0;

        float printTime = (float) (Math.round(currentTime * 10) / 10d);
        try {
            //Depending on the results the user chose...
            if (outResults.contains("tb")) {
                //Open the file
                out = new BufferedWriter(new FileWriter(resultsPath + File.separator + "Throughput_bits.txt", true));

                //Write the time
                out.write(printTime + "\t\t");

                //For each node
                for (int i = 0; i < nmbrOfNodes; i++) {
                    //Write the statistic
                    out.write(Integer.toString((int) getThrBps(nodesList.elementAt(i).params.id)) + "\t");
                }
                //Print the total value and change line
                out.write(Integer.toString((int) getSysThrBps()) + "\r\n");
                out.close();
            }

            if (outResults.contains("tp")) {
                //Open the file
                out = new BufferedWriter(new FileWriter(resultsPath + File.separator + "Throughput_Packets.txt", true));
                out.write(printTime + "\t\t");

                for (int i = 0; i < nmbrOfNodes; i++) {
                    out.write(Integer.toString((int) getThrPkts(nodesList.elementAt(i).params.id)) + "\t");
                }
                out.write(Integer.toString((int) getSysThrPkts()) + "\r\n");
                out.close();
            }

            if (outResults.contains("ut")) {
                out = new BufferedWriter(new FileWriter(resultsPath + File.separator + "Utilization.txt", true));
                out.write(printTime + "\t\t");

                for (int i = 0; i < nmbrOfNodes; i++) {
                    //Round the value in order to be printed correctly
                    result = (float) (Math.round(getUtil(nodesList.elementAt(i).params.id) * 10000) / 10000d);
                    out.write(Float.toString(result) + "\t");
                }
                result = (float) (Math.round(getSysUtil() * 10000) / 10000d);
                out.write(Float.toString(result) + "\r\n");
                out.close();
            }

            if (outResults.contains("md")) {
                out = new BufferedWriter(new FileWriter(resultsPath + File.separator + "Media_Access_Delay.txt", true));
                out.write(printTime + "\t\t");
                for (int i = 0; i < nmbrOfNodes; i++) {
                    result = (float) (Math.round(getMDelay(nodesList.elementAt(i).params.id) * 100) / 100d);
                    out.write(Float.toString(result) + "\t");
                }
                out.write("\r\n");
                out.close();
            }

            if (outResults.contains("qd")) {
                out = new BufferedWriter(new FileWriter(resultsPath + File.separator + "Queuing_Delay.txt", true));
                out.write(printTime + "\t\t");
                for (int i = 0; i < nmbrOfNodes; i++) {
                    //Round the value in order to be printed correctly
                    result = (float) (Math.round(getQDelay(nodesList.elementAt(i).params.id) * 100) / 100d);
                    out.write(Float.toString(result) + "\t");
                }
                out.write("\r\n");
                out.close();
            }

            if (outResults.contains("td")) {
                out = new BufferedWriter(new FileWriter(resultsPath + File.separator + "Total_Delay.txt", true));
                out.write(printTime + "\t\t");
                for (int i = 0; i < nmbrOfNodes; i++) {
                    //Round the value in order to be printed correctly
                    result = (float) (Math.round(getDelay(nodesList.elementAt(i).params.id) * 100) / 100d);
                    out.write(Float.toString(result) + "\t");
                }
                out.write("\r\n");
                out.close();
            }

            if (outResults.contains("dj")) {
                out = new BufferedWriter(new FileWriter(resultsPath + File.separator + "Jitter.txt", true));
                out.write(printTime + "\t\t");
                for (int i = 0; i < nmbrOfNodes; i++) {
                    //Round the value in order to be printed correctly
                    result = (float) (Math.round(getJitter(nodesList.elementAt(i).params.id) * 100) / 100d);
                    out.write(Float.toString(result) + "\t");
                }
                out.write("\r\n");
                out.close();
            }
            if (outResults.contains("ql")) {
                out = new BufferedWriter(new FileWriter(resultsPath + File.separator + "Queue_Length.txt", true));
                out.write(printTime + "\t\t");
                for (int i = 0; i < nmbrOfNodes; i++) {
                    out.write(Integer.toString((int) getQLength(nodesList.elementAt(i).params.id)) + "\t");
                }
                out.write("\r\n");
                out.close();
            }

            if (outResults.contains("ra")) {
                out = new BufferedWriter(new FileWriter(resultsPath + File.separator + "Retransmission_Attempts.txt", true));
                out.write(printTime + "\t\t");
                for (int i = 0; i < nmbrOfNodes; i++) {
                    //Round the value in order to be printed correctly
                    result = (float) (Math.round(getRatts(nodesList.elementAt(i).params.id) * 1000) / 1000d);
                    out.write(Float.toString(result) + "\t");
                }
                out.write("\r\n");
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ElementDoesNotExistException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    ////////////////////////////////////////////////////////////////////////
    //	*****		PadBits		*****
    //Calculates the Pad bits of an OFDM transmission.
    //The calculation is performed according to page 15 of the 802.11a spec.
    //
    ////////////////////////////////////////////////////////////////////////
    private int padBits(int rate, int psdu) {
        int NDBPS = 0;
        int NSYM = 0;
        int NDATA = 0;

        switch (rate) {
            case 6000000:
                NDBPS = 24;
            case 9000000:
                NDBPS = 36;
            case 12000000:
                NDBPS = 48;
            case 18000000:
                NDBPS = 72;
            case 24000000:
                NDBPS = 96;
            case 36000000:
                NDBPS = 144;
            case 48000000:
                NDBPS = 192;
            case 54000000:
                NDBPS = 216;
            default:
                NDBPS = 1; //Generally this is false, as ERP nodes do not support non-ERP rates.
            //But in any case the user makes a mistake...
        }

        NSYM = (int) Math.ceil((double) (16 + psdu + 6) / (double) NDBPS);
        NDATA = NSYM * NDBPS;
        return NDATA - (16 + psdu + 6);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    //			*******	ResetResultCounters	*******
    //This function resets the counters that hold the results at the end of each simulation interval.
    //Called before the simulate function. It has no meaning if simulate is called only once.
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    private void resetResultCounters() {
        //Fore each node...
        for (int i = 0; i < nmbrOfNodes; i++) {
            //Calculate each statistic
            nodesList.elementAt(i).successfulBits = 0;
            nodesList.elementAt(i).transmissionDuration = 0;
            nodesList.elementAt(i).successfulTransmissions = 0;
            nodesList.elementAt(i).queuingDelay = 0;
            nodesList.elementAt(i).jitter = 0;
            nodesList.elementAt(i).queueLength = 0;
            nodesList.elementAt(i).collisions = 0;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////*******************  Freeze   ********************************///////////////////////
    //Executes the freeze procedure of the 802.11 protocol, where the node stops decreasing the counter/////
    //when another node transmits.//////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void freeze() {
        int thisDur = 0; // Helpful variable.
        MobileNode n = null;
        transTimeRemaining--;
        if (transTimeRemaining == 0) {
            // Disable the transmission pending flag (no transmission
            // in progress).
            transmissionPending = false;

            // Disable the transWithRTS flag, which shows that
            // the station transmitted with RTS/CTS enabled.
            transmitWithRTS = false;

            for (int i = 0; i < nmbrOfNodes; i++) {
                n = (MobileNode) nodesList.elementAt(i);
                // Disable the nowTransmitting flag.
                n.nowTransmitting = false;

                // If a station finishes a successful transmission:
                if (n.successfullyTransmitting == true) {
                    n.successfullyTransmitting = false;
                    // Increase the number of successfully
                    // transmitted packets and bits.
                    n.successfulTransmissions++;
                    n.successfulBits += n.pktLength;

                    // Calculate the duration of the transmission
                    // (media access delay) and add it to the statistic.
                    n.transmissionDuration += MobileNode.timer - n.transmissionStart + 1;

                    // Calculate the total delay of the transmission,
                    thisDur = (int) (MobileNode.timer - n.getPacketBuffer().firstPacket().generationTime);

                    // find it's square and add it to the jitter statistic.
                    // This will be divided at the end with the mean total
                    // delay to find the delay jitter.
                    n.jitter += (int) Math.pow((double) thisDur, (double) 2);

                    // Remove the packet from the packet queue.
                    n.getPacketBuffer().dequeue();

                    // Disable the 'have packet to send flag, which means
                    // that the station has no packet in the transmitter.
                    n.havePktToSend = false;

                    // Mark the time the packet was successfully
                    // transmitted. (used for the backoff procedure).
                    n.lastPktTrans = MobileNode.timer;
                }
            }
        }

    }

    //////////////////////////////////////////////////////////////////////////////////////////
    ////////////// ************** TakePacketFromQueue *****************************///////////
    //Takes a packet from the packet buffer and puts it to the transmitter for transmission ///
    //////////////////////////////////////////////////////////////////////////////////////////
    private void takePacketFromQueue(int i) {
        int idleDur = 0;
        MobileNode n = (MobileNode) nodesList.elementAt(i);

        // Get the length of the packet (holded in the queue, not the source).
        n.pktLength = n.getPacketBuffer().firstPacket().length;

        // Calculate the queuing delay of the packet.
        n.queuingDelay += MobileNode.timer - n.getPacketBuffer().firstPacket().generationTime;
        // Do not yet remove the packet from the queue.
        // This will be done when its transmission is finished.
        // Initialize the contention window and the backoff counter
        // depending on the time the last packet was transmitted.
        idleDur = (int) (MobileNode.timer - n.lastPktTrans);

        // If the last packet was transmitted before a DIFS time
        if (idleDur > (int) (n.params.aifsd / slot)) {
            n.contWind = n.params.cwMin;
            n.backoffCounter = 0;
        } else {
            n.contWind = n.params.cwMin;
            n.backoffCounter = n.InitBackoff(n.params.cwMin);
        }

        // Mark the time the packet is picked up from the queue for
        // transmission. Used for the calculation of the transmission
        // delay statistic.
        n.transmissionStart = MobileNode.timer;

        // Enable the flag 'have packet to send' which means that the node
        // has a packet for transmission in the transmitter.
        n.havePktToSend = true;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    //		******	UpdateMeanResults	*******
    //This fuction adds the values of the result counters to the counters that hold the results for the
    //whole simulation. It is used only for calculating the mean values of each statistic at the end of the
    //simulation. It is called right after the end of the simulate function.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void updateMeanResults() {
        MobileNode n = null;
        for (int i = 0; i < nmbrOfNodes; i++) {
            n = nodesList.elementAt(i);

            n.totCollisions += n.collisions;
            collisionsForAllNodes += n.collisions;//added by simon
            n.totJitter += n.jitter;
            n.totQueueLength += n.queueLength;
            n.totQueuingDelay += n.queuingDelay;

            n.totSuccessfulBits += n.successfulBits;
            n.totSuccessfulTransmissions += n.successfulTransmissions;
            successForAllNodes += n.successfulTransmissions;//added by simon
            n.totTransmissionDurations += n.transmissionDuration;

        }
    }

    /**
     * Finds a node whose Id is 'nodeId', or null if the node with the specified ID does not exist.
     * @param nodeId The ID of the node.
     * @return The node whose ID is 'nodeId'
     * @throws ElementDoesNotExistException
     */
    public MobileNode getNode(int nodeId) throws ElementDoesNotExistException {
        MobileNode nd = null;
        for (int i = 0; i < nodesList.size(); i++) {
            if (nodesList.elementAt(i).params.id == nodeId) {
                nd = nodesList.elementAt(i);
                break;
            }
        }
        if (nd == null) {
            throw new ElementDoesNotExistException("Node " + nodeId + " does not exist.");
        }
        return nd;
    }

    /**
     * Simulates a specific time interval. First, this interval is simulated and then results are
     * collected. These results refer to this interval only. Be careful on how to call the simulate function.
     * e.g. For simulating from 0 to 10sec: simulate(1,10000). For simulating from 10sec to 20sec: simulate(10001,20000)
     * @param startTime The start time of the simulation interval in miliseconds.
     * @param endTime The end time of the simulation interval in miliseconds.
     */
    public void simulate(long startTime, long endTime) {
        MobileNode n = null;
        // Reset the counters that hold the simulation results.
        // Useful only if the simulate() method is called many times from the
        // interface. It has no effect
        // if the simulator is called only once (like the original version of
        // Pamvotis).
        resetResultCounters();
        collisionsForAllNodes = 0;
        successForAllNodes = 0;
        // Calculate the duration if the current simulation interval.
        // Remember that startTime and endTime are set in milliseconds.
        simTime = (long) ((endTime - startTime + 1) / slot / 1000);

        // Transform startTime and endTime in slots.
        startTime = (long) ((startTime - 1) / slot / 1000 + 1);
        endTime = (long) (endTime / slot / 1000);
        Vector<Packet> newPackets = null;
        // Start the simulation
        for (long currentSlot = startTime; currentSlot <= endTime; currentSlot++) {

            // Synchronize the timer of each node with the current slot.
            // nodesList.elementAt(i).timer = currentSlot;
            MobileNode.timer = currentSlot;
            Source.timer = currentSlot;

            //Configure the nodes
            for (int i = 0; i < nmbrOfNodes; i++) {
                n = (MobileNode) nodesList.elementAt(i);
                for (int j = 0; j < n._srcManager._vActiveSources.size(); j++) {
                    n._srcManager._vActiveSources.elementAt(j).synchronize();
                }

                //Take a packet from each source, if exists
                newPackets = n.pollPacketsFromSources();

                //Put the polled packets to the packet buffer
                if (!newPackets.isEmpty()) {
                    n.getPacketBuffer().enqueue(newPackets);
                }

                // If no packet is under transmission and if a packet
                // exists in the queue, take the packet from the queue
                // for transmission.
                if ((n.havePktToSend == false) && (n.getPacketBuffer().isEmpty() == false)) {
                    takePacketFromQueue(i);
                }

                // Calculate the queue length.
                // Add the current queue length to the statistic.
                // At the end it will be divided with the number of slots in
                // order to find the mean queue length.
                n.queueLength += (float) n.getPacketBuffer().size();
            }
            // Begin to compete for the medium (empty slot, successful
            // transmission or collision).
            fightForSlot();

            // This variable gives the time to the GUI for the interface or the
            // program that calls the Simulator instance.
            currentTime = (float) (currentSlot * slot);

            // This variable gives the percentage progress to the GUI for the
            // progress bar
            progress = (short) (currentSlot * slot / (float) totalTime * 100);
            // Some times rounding problems exist, causing progress to reach
            // until 99%.
            // The above statement solves the problem.
            if (currentTime == (float) totalTime) {
                progress = 100;
            }
        }

        // Add the values of the result counters to the counters that hold the
        // total results
        // for the whole simulation, in order to calculate the mean values at
        // the end.
        updateMeanResults();
        // Print the statistics of the current simulation interval to the files.
        printStats();
    }

    /**
     * Prints the headers of the files which contain the statistic results
     * Each statistic is printed only if the user asks it (depending on the
     * containing characteristic two-character sets of the reslts string.
     * Called from the interface.
     */
    public void printHeaders() {

        try {
            //Print the headers of the 'throughput in bits' file.
            if (outResults.contains("tb")) {
                out = new BufferedWriter(new FileWriter(resultsPath + File.separator + "Throughput_bits.txt"));

                //MAJOR: USE \r\n INSTEAD OF \n

                out.write("\t\t\t*****\t Throughput (Kbits/s)\t*****\r\n\r\n\r\n");
                out.write("Time (sec)\t");
                for (int i = 1; i <= nmbrOfNodes; i++) {
                    out.write("Node " + i + "\t");
                }
                out.write("System\r\n");
                out.close();
            }

            //Print the headers of the 'throughput in packets' file.
            if (outResults.contains("tp")) {
                out = new BufferedWriter(new FileWriter(resultsPath + File.separator + "Throughput_Packets.txt"));
                out.write("\t\t\t*****\t Throughput (packets/s)\t*****\r\n\r\n\r\n");
                out.write("Time (sec)\t");
                for (int i = 1; i <= nmbrOfNodes; i++) {
                    out.write("Node " + i + "\t");
                }
                out.write("System\r\n");
                out.close();
            }

            //Print the headers of the 'utilization' file.
            if (outResults.contains("ut")) {
                out = new BufferedWriter(new FileWriter(resultsPath + File.separator + "Utilization.txt"));
                out.write("\t\t\t*****\t Utilization\t*****\r\n\r\n\r\n");
                out.write("Time (sec)\t");
                for (int i = 1; i <= nmbrOfNodes; i++) {
                    out.write("Node " + i + "\t");
                }
                out.write("System\r\n");
                out.close();
            }

            //Print the headers of the 'media access delay' file.
            if (outResults.contains("md")) {
                out = new BufferedWriter(new FileWriter(resultsPath + File.separator + "Media_Access_Delay.txt"));
                out.write("\t\t\t*****\t Media Access Delay (msec) \t*****\r\n\r\n\r\n");
                out.write("Time (sec)\t");
                for (int i = 1; i <= nmbrOfNodes; i++) {
                    out.write("Node " + i + "\t");
                }
                out.write("\r\n");
                out.close();
            }

            //Print the headers of the 'queuing delay' file.
            if (outResults.contains("qd")) {
                out = new BufferedWriter(new FileWriter(resultsPath + File.separator + "Queuing_Delay.txt"));
                out.write("\t\t\t*****\t Queuing Delay (msec) \t*****\r\n\r\n\r\n");
                out.write("Time (sec)\t");
                for (int i = 1; i <= nmbrOfNodes; i++) {
                    out.write("Node " + i + "\t");
                }
                out.write("\r\n");
                out.close();
            }

            //Print the headers of the 'total delay' file.
            if (outResults.contains("td")) {
                out = new BufferedWriter(new FileWriter(resultsPath + File.separator + "Total_Delay.txt"));
                out.write("\t\t\t*****\t Total Packet Delay (msec) \t*****\r\n\r\n\r\n");
                out.write("Time (sec)\t");
                for (int i = 1; i <= nmbrOfNodes; i++) {
                    out.write("Node " + i + "\t");
                }
                out.write("\r\n");
                out.close();
            }

            //Print the headers of the 'delay Jitter' file.
            if (outResults.contains("dj")) {
                out = new BufferedWriter(new FileWriter(resultsPath + File.separator + "Jitter.txt"));
                out.write("\t\t\t*****\t Delay Jitter (msec) \t*****\r\n\r\n\r\n");
                out.write("Time (sec)\t");
                for (int i = 1; i <= nmbrOfNodes; i++) {
                    out.write("Node " + i + "\t");
                }
                out.write("\r\n");
                out.close();
            }

            //Print the headers of the 'queue length' file.
            if (outResults.contains("ql")) {
                out = new BufferedWriter(new FileWriter(resultsPath + File.separator + "Queue_Length.txt"));
                out.write("\t\t\t*****\t Packet Queue Length\t*****\r\n\r\n\r\n");
                out.write("Time (sec)\t");
                for (int i = 1; i <= nmbrOfNodes; i++) {
                    out.write("Node " + i + "\t");
                }
                out.write("\r\n");
                out.close();
            }

            //Print the headers of the 'Retransmission Attempts' file.
            if (outResults.contains("ra")) {
                out = new BufferedWriter(new FileWriter(resultsPath + File.separator + "Retransmission_Attempts.txt"));
                out.write("\t\t\t*****\t Retransmission Attempts\t*****\r\n\r\n\r\n");
                out.write("Time (sec)\t");
                for (int i = 1; i <= nmbrOfNodes; i++) {
                    out.write("Node " + i + "\t");
                }
                out.write("\r\n");
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Prints the mean statistic results to text file. These mean results concern the whole simulation
     * and all the times the simulate() method was called.
     */
    public void printMeanValues() {

        float thrBt = 0, thrPkt = 0, util = 0, mDel = 0, qDel = 0, tDel = 0, jitter = 0, rAtts = 0, qLngth = 0;
        float thrTotBt = 0;
        float thrTotPkt = 0;
        float utilTot = 0;

        try {
            out = new BufferedWriter(new FileWriter(resultsPath + File.separator + "Mean_Values.txt"));
            //Open the file and print the headers
            out.write("\t\t\t*****\t Mean Statistic Values\t*****\r\n\r\n");
            out.write("Node\tThroughput\tThroughput\t");
            out.write("Utilization\tMedia Access Delay\t");
            out.write("Queuing Delay\tTotal Packet Delay\tDelay Jitter\t");
            out.write("Queue Length\tRetransmission Attempts\r\n");
            out.write("\t(Kbits/s)\t(packets/s)\t\t\t(msec)\t\t\t(msec)");
            out.write("\t\t(msec)\t\t\t(msec)\r\n");

            //Fore each node...
            for (int i = 0; i < nmbrOfNodes; i++) {
                //Calculate each statistic
                thrBt = (float) nodesList.elementAt(i).totSuccessfulBits / (float) totalTime / 1000;
                thrTotBt += thrBt;
                thrPkt = (float) nodesList.elementAt(i).totSuccessfulTransmissions / (float) totalTime;
                thrTotPkt += thrPkt;
                util = (float) thrBt * 1000 / (float) nodesList.elementAt(i).params.rate;
                utilTot += util;
                mDel = (float) nodesList.elementAt(i).totTransmissionDurations
                        / (float) nodesList.elementAt(i).totSuccessfulTransmissions;
                mDel = mDel * slot * 1000;
                qDel = (float) nodesList.elementAt(i).totQueuingDelay
                        / (float) nodesList.elementAt(i).totSuccessfulTransmissions;
                qDel = qDel * slot * 1000;
                tDel = mDel + qDel;
                jitter = (float) nodesList.elementAt(i).totJitter
                        / (float) nodesList.elementAt(i).totSuccessfulTransmissions
                        - (float) Math.pow((float) tDel, 2);
                jitter = (float) Math.sqrt((float) jitter);
                jitter = jitter * slot * 1000;
                qLngth = (float) nodesList.elementAt(i).totQueueLength / (float) totalTime * slot;
                rAtts = (float) nodesList.elementAt(i).totCollisions
                        / (float) nodesList.elementAt(i).totSuccessfulTransmissions;
                //Round the values in order to be printed correctly
                thrBt = (float) Math.round(thrBt);
                thrPkt = (float) Math.round(thrPkt);
                util = (float) (Math.round(util * 10000) / 10000d);
                mDel = (float) (Math.round(mDel * 100) / 100d);
                qDel = (float) (Math.round(qDel * 100) / 100d);
                tDel = (float) (Math.round(tDel * 100) / 100d);
                jitter = (float) (Math.round(jitter * 100) / 100d);
                rAtts = (float) (Math.round(rAtts * 1000) / 1000d);

                //Print the values to the file
                out.write(i + 1 + "\t" + (int) thrBt + "\t\t");
                out.write((int) thrPkt + "\t\t" + util + "\t\t");
                out.write(mDel + "\t\t\t" + qDel + "\t\t" + tDel + "\t\t\t");
                out.write(jitter + "\t\t" + (int) qLngth + "\t\t" + rAtts + "\r\n");
            }

            //Round the values of the global utilization statistics
            thrTotBt = (float) Math.round(thrTotBt);
            thrTotPkt = (float) Math.round(thrTotPkt);
            utilTot = (float) (Math.round(utilTot * 10000) / 10000d);

            //Print some statistics that concern the system globally.
            out.write("\r\nSystem\t" + (int) thrTotBt + "\t\t");
            out.write((int) thrTotPkt + "\t\t" + utilTot + "\t\t");

            //close the file
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    //		*****	GET FUNCTIONS FOR THE RESULTS		**********
    //The following functions calculate the results for each statistic.
    //They are called from the printStats function in order to print the results to files,
    //after each simulation interval.
    //However, they can be called from the external interface (or another simulator that uses Pamvotis).
    //This is why they are public.
    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * @return The % progress of the simulation
     */
    public int getProgress() {
        return progress;
    }

    /**
     * @return The simulation time in seconds
     */
    public long getTime() {
        return Math.round(currentTime);

    }

    /**
     * Get the throughput of a node for the last simulation interval.
     * @param node The node for which we ask for the throughput
     * @return The throughput of the node in Kb/s
     * @throws ElementDoesNotExistException
     */
    public float getThrBps(int node) throws ElementDoesNotExistException {
        return (float) (getNode(node).successfulBits
                / (simTime * slot * 1000));
    }

    /**
     * Get the throughput of the system for the last simulation interval.
     * @return The throughput of the system in Kb/s
     */
    public float getSysThrBps() {
        float result = 0;
        for (int i = 0; i < nmbrOfNodes; i++) {
            result += (float) (nodesList.elementAt(i).successfulBits
                    / (simTime * slot * 1000));
        }
        return result;
    }

    /**
     * Get the throughput of a node for the last simulation interval.
     * @param node The node for which we ask for the throughput
     * @return The throughput of the node in packets/s
     * @throws ElementDoesNotExistException
     */
    public float getThrPkts(int node) throws ElementDoesNotExistException {
        return (float) (getNode(node).successfulTransmissions
                / (double) simTime) / slot;
    }

    /**
     * Get the throughput of the system for the last simulation interval.
     * @return The throughput of the system in packets/s
     */
    public float getSysThrPkts() {
        float result = 0;
        for (int i = 0; i < nmbrOfNodes; i++) {
            result += (float) (nodesList.elementAt(i).successfulTransmissions
                    / (double) simTime) / slot;
        }
        return result;
    }

    /**
     * Get the utilization of a node for the last simulation interval.
     * @param node The node for which we ask for the utilization
     * @return The utilization of the node in a percentage value
     * @throws ElementDoesNotExistException
     */
    public float getUtil(int node) throws ElementDoesNotExistException {
        return (float) (getNode(node).successfulBits
                / (simTime * slot * getNode(node).params.rate));
    }

    /**
     * Get the utilization of the system for the last simulation interval.
     * @return The utilization of the system in a percentage value
     */
    public float getSysUtil() {
        float resultTot = 0;

        for (int i = 0; i < nmbrOfNodes; i++) {
            resultTot += (float) (nodesList.elementAt(i).successfulBits
                    / (simTime * slot * nodesList.elementAt(i).params.rate));
        }
        return resultTot;
    }

    /**
     * Get the media access delay of a node for the last simulation interval. The media access delay is defined
     * as the delay from the time a packet is picked up for transmission, until the ACK from the receiving node
     * arrives to the sending node.
     * @param node The node for which we ask for the media access delay
     * @return The media access delay of the node in miliseconds.
     * @throws ElementDoesNotExistException
     */
    public float getMDelay(int node) throws ElementDoesNotExistException {
        try {
            float result = (float) (getNode(node).transmissionDuration
                    / getNode(node).successfulTransmissions);
            return result * slot * 1000;
        } catch (ArithmeticException e) {
            return 0;
        }
    }

    /**
     * Get the queuing delay of a node for the last simulation interval. The queuing delay is defined
     * as the delay from the time a packet is generated, until the packet is picked up from the transmitter
     * for transmission.
     * @param node The node for which we ask for the queuing delay
     * @return The queuing delay of the node in miliseconds.
     * @throws ElementDoesNotExistException
     */
    public float getQDelay(int node) throws ElementDoesNotExistException {
        try {
            float result = (float) (getNode(node).queuingDelay
                    / getNode(node).successfulTransmissions);
            return result * slot * 1000;
        } catch (ArithmeticException e) {
            return 0;
        }
    }

    /**
     * Get the total delay of a node for the last simulation interval. The total delay is defined
     * as the sum of the media access delay and the queuing delay.
     * @param node The node for which we ask for the total delay
     * @return The total delay of the node in miliseconds.
     * @throws ElementDoesNotExistException
     */
    public float getDelay(int node) throws ElementDoesNotExistException {
        return getMDelay(node) + getQDelay(node);
    }

    /**
     * Get the jitter of a node for the last simulation interval.
     * @param node The node for which we ask for the jitter.
     * @return The jitter of the node in miliseconds.
     * @throws ElementDoesNotExistException
     */
    public float getJitter(int node) throws ElementDoesNotExistException {
        try {
            float result = (float) (getNode(node).jitter
                    / getNode(node).successfulTransmissions)
                    - (float) Math.pow((float) getDelay(node), 2);
            result = (float) Math.sqrt((float) result);
            result = (float) (result * slot * 1000);
            return result;
        } catch (ArithmeticException e) {
            return 0;
        }
    }

    /**
     * Get the average packet buffer length of a node for the last simulation interval.
     * @param node The node for which we ask for the buffer length.
     * @return The buffer length of the node in packets.
     * @throws ElementDoesNotExistException
     */
    public float getQLength(int node) throws ElementDoesNotExistException {
        return (float) (getNode(node).queueLength / simTime);
    }

    /**
     * Get the average number of retransmission attempts of a node for the last simulation interval.
     * @param node The node for which we ask for the average number of retransmission attempts.
     * @return The average number of retransmission attempts (pure value).
     * @throws ElementDoesNotExistException
     */
    public float getRatts(int node) throws ElementDoesNotExistException {
        try {
            return (float) getNode(node).collisions
                    / (float) getNode(node).successfulTransmissions;
        } catch (ArithmeticException e) {
            return 0;
        }
    }

    /**
     * Changes the parameters of a node. Can be called anywhere on the simulation. This method is useful
     * if a mobility or signal strength pattern was implemented, currently not supported in Pamvotis.
     * @param node The node for which we want to change the parameters.
     * @param coverage The node's coverage in meters.
     * @param xPosition The node's x-axis coordinate in meters.
     * @param yPosition The node's y-axis coordinate in meters.
     */
    public void changeNodeParams(int node, int coverage, int xPosition,
            int yPosition) {
        MobileNode n = (MobileNode) nodesList.elementAt(node);
        if (n == null) {
            return;
        }
        if (coverage != -1) {
            n.params.coverage = coverage;
        }
        if (xPosition != -1) {
            n.params.x = xPosition;
        }
        if (yPosition != -1) {
            n.params.y = yPosition;
        }
    }

    /**
     * Configures system and node parameters according to the "config/NtConf.xml" configuration file.
     * If this method is used externally, pay attention on where to store the xml file.
     */
    public void confParams() {

        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse("config" + File.separator + "NtConf.xml");

            // First read the system parameters.
            seed = Integer.parseInt(doc.getElementsByTagName("seed").item(0).getTextContent());
            totalTime = Integer.parseInt(doc.getElementsByTagName("duration").item(0).getTextContent());
            mixNodes = Integer.parseInt(doc.getElementsByTagName("mixNodes").item(0).getTextContent());
            rtsThr = Integer.parseInt(doc.getElementsByTagName("RTSThr").item(0).getTextContent());
            ctsToSelf = doc.getElementsByTagName("ctsToSelf").item(0).getTextContent().charAt(0);
            phyLayer = doc.getElementsByTagName("phyLayer").item(0).getTextContent().charAt(0);
            resultsPath = doc.getElementsByTagName("resultsPath").item(0).getTextContent();
            outResults = doc.getElementsByTagName("outResults").item(0).getTextContent();

            // Depending on the physical layer define the value of each
            // parameter.
            // In 802.11a and pure 802.11g (ERP true)...
            if ((phyLayer == 'a') || (phyLayer == 'g')) {
                slot = (float) SpecParams.SLOT_ERP;
                cwMin = SpecParams.CW_MIN_OFDM;
            } // In 802.11, 802.11b or mixed 802.11g - 802.11b
            else {
                slot = (float) SpecParams.SLOT_NON_ERP;
                cwMin = SpecParams.CW_MIN_DSSS; /* default cwMin */
            }

            // Sifs parameter is 16us for 802.11a
            if (phyLayer == 'a') {
                sifs = (float) SpecParams.SIFS_A;
            } else {
                sifs = (float) SpecParams.SIFS_G;
            }

            // Read the 802.11e parameters
            cwMinFact0 = Integer.parseInt(doc.getElementsByTagName("cwMinFact0").item(0).getTextContent());
            cwMinFact1 = Integer.parseInt(doc.getElementsByTagName("cwMinFact1").item(0).getTextContent());
            cwMinFact2 = Integer.parseInt(doc.getElementsByTagName("cwMinFact2").item(0).getTextContent());
            cwMinFact3 = Integer.parseInt(doc.getElementsByTagName("cwMinFact3").item(0).getTextContent());

            cwMaxFact0 = Integer.parseInt(doc.getElementsByTagName("cwMaxFact0").item(0).getTextContent());
            cwMaxFact1 = Integer.parseInt(doc.getElementsByTagName("cwMaxFact1").item(0).getTextContent());
            cwMaxFact2 = Integer.parseInt(doc.getElementsByTagName("cwMaxFact2").item(0).getTextContent());
            cwMaxFact3 = Integer.parseInt(doc.getElementsByTagName("cwMaxFact3").item(0).getTextContent());

            aifs0 = Integer.parseInt(doc.getElementsByTagName("aifs0").item(0).getTextContent());
            aifs1 = Integer.parseInt(doc.getElementsByTagName("aifs1").item(0).getTextContent());
            aifs2 = Integer.parseInt(doc.getElementsByTagName("aifs2").item(0).getTextContent());
            aifs3 = Integer.parseInt(doc.getElementsByTagName("aifs3").item(0).getTextContent());

            // Then we must get the number of nodes.
            NodeList nodes = doc.getElementsByTagName("node");
            //nmbrOfNodes = nodes.getLength();
            nmbrOfNodes = 0;
            //Initialize the random number generator. This must be done before creating the node
            //because we pass the generator as an argument to its constructor.
            generator = new java.util.Random((long) (seed));

            // Set static parameters required by all Source types
            Source.generator = generator;
            MobileNode.generator = generator;
            Source.slot = slot;

            // For each node we must read it's parameters and store them.
            for (int i = 0; i < nodes.getLength(); i++) {

                // Read the node's parameters
                Node node = nodes.item(i);
                Element ndElement = (Element) node;
                NamedNodeMap attrs = node.getAttributes();
                int id = Integer.parseInt(attrs.getNamedItem("number").getNodeValue());
                int rate = Integer.parseInt(ndElement.getElementsByTagName("rate").item(0).getTextContent());
                int coverage = Integer.parseInt(ndElement.getElementsByTagName("coverage").item(0).getTextContent());
                int xPosition = Integer.parseInt(ndElement.getElementsByTagName("xPosition").item(0).getTextContent());
                int yPosition = Integer.parseInt(ndElement.getElementsByTagName("yPosition").item(0).getTextContent());
                int ac = Integer.parseInt(ndElement.getElementsByTagName("AC").item(0).getTextContent());
                try {
                    addNode(id, rate, coverage, xPosition, yPosition, ac);
                } catch (ElementExistsException e) {
                    throw new ConfigurationException("You have already configured a node with ID " + id + ". Please check your network configuration file.");
                }
                //Read the node's sources
                NodeList sourceList = ndElement.getElementsByTagName("source");
                for (int j = 0; j < sourceList.getLength(); j++) {

                    Node source = sourceList.item(j);
                    Element sourceElement = (Element) source;
                    NamedNodeMap attributes = source.getAttributes();
                    int sourceId = Integer.parseInt(attributes.getNamedItem("id").getNodeValue());
                    try {
                        if (attributes.getNamedItem("type").getNodeValue().equals("generic")) {
                            float pktLnght = Float.parseFloat(sourceElement.getElementsByTagName("pktLngth").item(0).getTextContent());
                            float intArr = Float.parseFloat(sourceElement.getElementsByTagName("intArrTime").item(0).getTextContent());
                            char pktDist = sourceElement.getElementsByTagName("pktDist").item(0).getTextContent().charAt(0);
                            char intArrDstr = sourceElement.getElementsByTagName("intArrDstr").item(0).getTextContent().charAt(0);
                            //Add a generic source to the node
                            GenericSource s = null;
                            try {
                                s = new GenericSource(sourceId, intArrDstr, intArr, pktDist, pktLnght);
                            } catch (UnknownDistributionException ex) {
                                throw new ConfigurationException("The packet length or/and the packet interarrival time distribution(s) you have configured are invalid. Only 'c','u' and 'e' are allowed.");
                            }

                            appendNewSource(id, s);
                        } else if (attributes.getNamedItem("type").getNodeValue().equals("ftp")) {
                            int pktSize = Integer.parseInt(sourceElement.getElementsByTagName("pktSize").item(0).getTextContent());
                            float fileSizeMean = Float.parseFloat(sourceElement.getElementsByTagName("fileSizeMean").item(0).getTextContent());
                            float fileSizeStDev = Float.parseFloat(sourceElement.getElementsByTagName("fileSizeStDev").item(0).getTextContent());
                            float fileSizeMax = Float.parseFloat(sourceElement.getElementsByTagName("fileSizeMax").item(0).getTextContent());
                            float readingTime = Float.parseFloat(sourceElement.getElementsByTagName("readingTime").item(0).getTextContent());

                            //Add an FTP source to the node
                            FTPSource s = new FTPSource(sourceId, pktSize, fileSizeMean, fileSizeStDev, fileSizeMax, readingTime);
                            appendNewSource(id, s);
                        } else if (attributes.getNamedItem("type").getNodeValue().equals("video")) {
                            int frameRate = Integer.parseInt(sourceElement.getElementsByTagName("frameRate").item(0).getTextContent());
                            int packetsPerFrame = Integer.parseInt(sourceElement.getElementsByTagName("packetsPerFrame").item(0).getTextContent());
                            float pktSize = Float.parseFloat(sourceElement.getElementsByTagName("pktSize").item(0).getTextContent());
                            float pktSizeMax = Float.parseFloat(sourceElement.getElementsByTagName("pktSizeMax").item(0).getTextContent());
                            float pktIntArr = Float.parseFloat(sourceElement.getElementsByTagName("pktIntArr").item(0).getTextContent());
                            float pktIntArrMax = Float.parseFloat(sourceElement.getElementsByTagName("pktIntArrMax").item(0).getTextContent());

                            //Add a video source to the node
                            VideoSource s = new VideoSource(sourceId, frameRate, packetsPerFrame, pktSize, pktSizeMax, pktIntArr, pktIntArrMax);
                            appendNewSource(id, s);
                        } else if (attributes.getNamedItem("type").getNodeValue().equals("http")) {
                            int pktSize = Integer.parseInt(sourceElement.getElementsByTagName("pktSize").item(0).getTextContent());
                            float mainObjectMean = Float.parseFloat(sourceElement.getElementsByTagName("mainObjectMean").item(0).getTextContent());
                            float mainObjectStDev = Float.parseFloat(sourceElement.getElementsByTagName("mainObjectStDev").item(0).getTextContent());
                            float mainObjectMin = Float.parseFloat(sourceElement.getElementsByTagName("mainObjectMin").item(0).getTextContent());
                            float mainObjectMax = Float.parseFloat(sourceElement.getElementsByTagName("mainObjectMax").item(0).getTextContent());
                            float embObjectMean = Float.parseFloat(sourceElement.getElementsByTagName("embObjectMean").item(0).getTextContent());
                            float embObjectStDev = Float.parseFloat(sourceElement.getElementsByTagName("embObjectStDev").item(0).getTextContent());
                            float embObjectMin = Float.parseFloat(sourceElement.getElementsByTagName("embObjectMin").item(0).getTextContent());
                            float embObjectMax = Float.parseFloat(sourceElement.getElementsByTagName("embObjectMax").item(0).getTextContent());
                            float NumOfEmbObjectsMean = Float.parseFloat(sourceElement.getElementsByTagName("NumOfEmbObjectsMean").item(0).getTextContent());
                            float NumOfEmbObjectsMax = Float.parseFloat(sourceElement.getElementsByTagName("NumOfEmbObjectsMax").item(0).getTextContent());
                            float readingTime = Float.parseFloat(sourceElement.getElementsByTagName("readingTime").item(0).getTextContent());
                            float parsingTime = Float.parseFloat(sourceElement.getElementsByTagName("parsingTime").item(0).getTextContent());

                            //Add an HTTP source to the node
                            HTTPSource s = new HTTPSource(sourceId, pktSize, mainObjectMean, mainObjectStDev, mainObjectMin, mainObjectMax, embObjectMean,
                                    embObjectStDev, embObjectMin, embObjectMax, NumOfEmbObjectsMean, NumOfEmbObjectsMax, readingTime, parsingTime);
                            appendNewSource(id, s);
                        }
                    } catch (ElementExistsException ex) {
                        throw new ConfigurationException("You have already configured a source with ID " + sourceId + ". Check your network configuration file.");
                    } catch (ElementDoesNotExistException ex) {
                        throw new ConfigurationException("Node " + id + ", where you are trying to add source " + sourceId + ", already exists. Please check your network configuration file.");
                    }
                }
            }

        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (DOMException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ConfigurationException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Adds a new node to the system.
     * Pay attention: The user is responsible for assigning correct node IDs. If a dublicate ID exists in the system
     * the simulator is not responsible of identifying it, and unexpected results will occur.
     * @param id The ID of the new node.
     * @param rate The data rate of the node.
     * @param coverage The coverage of the node.
     * @param xPosition The x-axis coordinate.
     * @param yPosition The y-axis coordinate
     * @param ac The 802.11 EDCA access category.
     * @throws ElementExistsException
     */
    public void addNode(int id, int rate, int coverage, int xPosition, int yPosition, int ac) throws ElementExistsException {
        boolean nodeExists = false;
        for (int i = 0; i < nodesList.size(); i++) {
            if (nodesList.elementAt(i).params.id == id) {
                nodeExists = true;

                break;
            }
        }
        if (nodeExists) {
            throw new ElementExistsException("Node " + id + " already exists.");
        } else {
            MobileNode nd = new MobileNode();
            // Initialize the 802.11e parameters depending on the node's AC
            // Those parameters will be used in initParams function to
            // initialize the params class member variables.
            int nCwMin = cwMin;
            int nCwMax = SpecParams.CW_MAX;
            float nAifsd = sifs + 2 * slot;
            switch (ac) {
                case 1: {
                    nCwMin = (int) ((float) cwMin / (float) cwMinFact1);
                    nCwMax = (int) ((float) SpecParams.CW_MAX / (float) cwMaxFact1);
                    nAifsd = sifs + aifs1 * slot;
                    break;
                }
                case 2: {
                    nCwMin = (int) ((float) cwMin / (float) cwMinFact2);
                    nCwMax = (int) ((float) SpecParams.CW_MAX / (float) cwMaxFact2);
                    nAifsd = sifs + aifs2 * slot;
                    break;
                }
                case 3: {
                    nCwMin = (int) ((float) cwMin / (float) cwMinFact3);
                    nCwMax = (int) ((float) SpecParams.CW_MAX / (float) cwMaxFact3);
                    nAifsd = sifs + aifs3 * slot;
                    break;
                }
                default: { // case 0: if user makes a mistake it will be
                    // assumed best effort class
                    nCwMin = (int) ((float) cwMin / (float) cwMinFact0);
                    nCwMax = (int) ((float) SpecParams.CW_MAX / (float) cwMaxFact0);
                    nAifsd = sifs + aifs0 * slot;
                    break;
                }
            }
            nd.params.InitParams(id, rate, xPosition, yPosition, coverage, ac, nAifsd,
                    nCwMin, nCwMax);
            nd.contWind = nd.params.cwMin;
            nodesList.addElement(nd);
            nmbrOfNodes++;
        }
    }

    /**
     * Removes a node with a specific ID
     * @param nodeId The ID of the node to be removed.
     * @return True if the node removed successfully or false in other case.
     * @throws ElementDoesNotExistException
     */
    public boolean removeNode(int nodeId) throws ElementDoesNotExistException {
        int position = -1;
        for (int i = 0; i < nodesList.size(); i++) {
            if (nodesList.elementAt(i).params.id == nodeId) {
                position = i;
                break;
            }
        }
        if (position != -1) {
            nodesList.removeElementAt(position);
            nmbrOfNodes--;
            return true;
        } else {
            throw new ElementDoesNotExistException("Node " + nodeId + " does not exist.");
        }
    }

    /**
     * Removes all nodes from the system
     */
    public void removeAllNodes() {
        nodesList.clear();
        nmbrOfNodes = 0;
    }

    /**
     * Appends a new source to a specific node
     * @param node The node id, which is the element number of the vector that stores the nodes.
     * @param newSource A source instance
     * @return False if the node is not available.
     * @throws ElementExistsException
     * @throws ElementDoesNotExistException
     * @see pamvotis.sources.Source
     * @see #removeSource
     * @see #removeAllSources
     */
    public boolean appendNewSource(int node, Source newSource) throws ElementExistsException, ElementDoesNotExistException {

        MobileNode n = (MobileNode) getNode(node);
        if (n == null) {
            return false;
        }
        //newSource.getNextPacket();
        n.addSource(newSource);
        return true;

    }

    /**
     * Removes all sources from the specified node.
     * @param node The node id, which is the element number of the vector that stores the nodes.
     * @return False if the id does not correspond to an existing node
     * @throws ElementDoesNotExistException
     * @see #removeSource
     * @see #appendNewSource
     * @see MobileNode
     */
    public boolean removeAllSources(int node) throws ElementDoesNotExistException {
        MobileNode n = (MobileNode) getNode(node);
        if (n == null) {
            return false;
        }
        n.removeAllSources();
        return true;
    }

    /**
     * Removes a source from a given node
     * @param node The node id from which the source will be removed
     * @param sourceId The unique id of the session to be terminated
     * @throws ElementDoesNotExistException
     * @see pamvotis.sources.Source
     * @see #appendNewSource
     * @see #removeAllSources
     * @see MobileNode
     */
    public void removeSource(int node, int sourceId) throws ElementDoesNotExistException {
        MobileNode n = getNode(node);
        if (n != null) {
            n.removeSource(sourceId);
        }
    }

    /**
     * @return the currentStrategy
     */
    public int getCurrentStrategy() {
        return currentStrategy;
    }

    /**
     * @param currentStrategy the currentStrategy to set
     */
    public void setCurrentStrategy(int currentStrategy) {
        this.currentStrategy = currentStrategy;
    }

    /**
     * @return the collisionsForAllNodes
     */
    public long getCollisionsForAllNodes() {
        return collisionsForAllNodes;
    }

    /**
     * @return the successForAllNodes
     */
    public long getSuccessForAllNodes() {
        return successForAllNodes;
    }
}
