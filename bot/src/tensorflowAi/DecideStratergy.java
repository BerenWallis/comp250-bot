package tensorflowAi;

import org.tensorflow.*;

import rts.PhysicalGameState;
import rts.units.*;


public class DecideStratergy {

	int strat = 0;
	public DecideStratergy(UnitTypeTable utt, PhysicalGameState pgs,  int p)
	{	
		int mapData[][] = createMapData(pgs,utt,p); // [x+y*width location] [0 level | 1 units]
		try
		{
			Graph graph = new Graph(); 
			Session session = new Session(graph);		
			float[] probability = null;
			Tensor<?> t = Tensor.create(mapData);
			Tensor<Float> output =
					session
					.runner()
					.feed("encoded_image_bytes", t)
                    .fetch("probability")
                    .run()
                    .get(0)
                    .expect(Float.class);
            if (probability == null) 
            {
            	probability = new float[(int) output.shape()[0]];
            }
            output.copyTo(probability);
            session.close();
            strat = argmax(probability);
                    
		}
		catch(Exception e)
		{}
	}
	
	private int[][] createMapData(PhysicalGameState pgs, UnitTypeTable utt, int p)
	{
		int mapData[][] = new int[pgs.getWidth() * pgs.getHeight()][2];
		int numUnitTypes = utt.getUnitTypes().size();
		for(int y = 0; y < pgs.getHeight(); y++)
		{
			for(int x = 0; x< pgs.getWidth(); x++)
			{
				if(pgs.getTerrain(x, y)== PhysicalGameState.TERRAIN_NONE)
				{
					Unit u = pgs.getUnitAt(x, y);
					if(u != null)
					{
						if (u.getPlayer() == p)
						{
							mapData[x+y*pgs.getWidth()][1] = u.getType().ID + 2;
						}
						else
						{
							mapData[x+y*pgs.getWidth()][1] = u.getType().ID + numUnitTypes + 2;
						}
					}
					else
					{
						mapData[x+y*pgs.getWidth()][0] = 1;
					}
				}
				else
				{
					mapData[x+y*pgs.getWidth()][0] = 0;
				}
			}
		}
		return mapData;
		
	}
	private int argmax(float[] probabilities) {
	    int best = 0;
	    for (int i = 1; i < probabilities.length; ++i) {
	      if (probabilities[i] > probabilities[best]) {
	        best = i;
	      }
	    }
	    return best;
	  }
}
