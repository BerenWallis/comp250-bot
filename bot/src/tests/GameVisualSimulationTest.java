package tests;
 /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import ai.core.AI;
import ai.RandomAI;
import ai.RandomBiasedAI;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerRush;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.mcts.naivemcts.NaiveMCTS;
import bot.*;
import gui.PhysicalGameStatePanel;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import javax.swing.JFrame;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;
import util.XMLWriter;

/**
 *
 * @author santi
 */
public class GameVisualSimulationTest {
	static BufferedWriter bufferedWriter;
	static String MAP = "../microrts/maps/8x8/bases8x8.xml";
    public static void main(String args[]) throws Exception {
    	bufferedWriter = new BufferedWriter(new FileWriter("../bot/src/tensorflowAi/TrainingData.dat"));
        UnitTypeTable utt = new UnitTypeTable();
        PhysicalGameState pgs = PhysicalGameState.load(MAP, utt);
//        PhysicalGameState pgs = MapGenerator.basesWorkers8x8Obstacle();

        GameState gs = new GameState(pgs, utt);
        int MAXCYCLES = 5000;
        int PERIOD = 20;
        boolean gameover = false;
        
        AI ai2 = new RangedRush(utt, new BFSPathFinding());
        AI ai1 = new ShallowMind(utt, pgs);
        //AI ai2 = new RangedRush();

        JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,PhysicalGameStatePanel.COLORSCHEME_BLACK);
//        JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,PhysicalGameStatePanel.COLORSCHEME_WHITE);

        long nextTimeToUpdate = System.currentTimeMillis() + PERIOD;
        do{
            if (System.currentTimeMillis()>=nextTimeToUpdate) {
                PlayerAction pa1 = ai1.getAction(0, gs);
                PlayerAction pa2 = ai2.getAction(1, gs);
                gs.issueSafe(pa1);
                gs.issueSafe(pa2);

                // simulate:
                gameover = gs.cycle();
                w.repaint();
                nextTimeToUpdate+=PERIOD;
            } else {
                try {
                    Thread.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }while(!gameover && gs.getTime()<MAXCYCLES);
        writeTrainingData(MAP, Integer.toString(gs.winner()));
        System.out.println("Game Over");
    }
    static void writeTrainingData(String map, String outcome) throws IOException
    {
    	bufferedWriter.append(map + " - " + outcome);
    }
}
