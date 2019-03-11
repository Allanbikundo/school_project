/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package src.pamvotis.core.utils;

import java.security.SecureRandom;
import java.util.Random;

/**
 *
 * @author Amulongo
 */
public class Generic {

     protected static int getSecureRandomNumber(int lowerBound, int upperBound){

       SecureRandom random  = new SecureRandom();
       int secureNumber = 0;

       for (int i = lowerBound; i<=upperBound;i++){

           secureNumber = random.nextInt(1000);

       }
      return secureNumber;
   }

     public static int getRandomNumber (int bound){


         return (int) (Math.random() * bound);
     }



       public static int getRandomNumber (int lowerBound, int upperBound){


             Random random  = new Random();
             int upper = upperBound + 1;

            int R = random.nextInt(upper-lowerBound) + lowerBound;
         return R;
     }


    /*public static void main (String [] args) {

               System.out.println(getRandomNumber(5, 10));

      }

      */

}
