package bot;

import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedList;

import rts.*;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

import tensorflowAi.DecideStratergy;
import bot.Commands;
import bot.Commands.attachedCommandEnum;

public class ShallowMind extends AbstractionLayerAI 
{    
    private UnitTypeTable utt;
    private UnitType workerType;
    private UnitType baseType;
    private UnitType barracksType;
    private UnitType lightType;
    private UnitType heavyType;
    private UnitType rangedType;
    private UnitType resourceType;
 
    public ShallowMind(UnitTypeTable utt, PhysicalGameState pgs_in) 
    {
        super(new AStarPathFinding());
        
        DecideStratergy strat = new DecideStratergy(utt, pgs_in, 0);
        System.out.println(strat);
        this.utt = utt;
        workerType = utt.getUnitType("Worker");
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        lightType = utt.getUnitType("Light");        
        heavyType = utt.getUnitType("Heavy");
        rangedType = utt.getUnitType("Ranged");
        resourceType = utt.getUnitType("Resource");
    }
    
    //--- Global Variables
    private final int RESOURCE_CHECK = 7;
    private PhysicalGameState pgs;
    private GameState gs;
    private Player p;
    private LinkedList<Long> enemyBases = new LinkedList<Long>();
    private LinkedList<Long> playerBases = new LinkedList<Long>();
    HashMap<Long, Commands> commandMap = new HashMap<Long, Commands>(); // contains Units and commands
    
    @Override
    public void reset() {}
    
    @Override
    public AI clone() 
    {
        return new ShallowMind(utt);
    }
   
    @Override
    public PlayerAction getAction(int player, GameState gs_in) 
    {
        pgs = gs.getPhysicalGameState();
        
        p = gs.getPlayer(player);
        
        for (Unit u : pgs.getUnits()) 
        {
        	if(u.getPlayer() == player)
        	{
        		if(u.getType() == baseType)
        		{
        			playerBases.add(u.getID());
        		}
        	}
        	
        	//Adding Enemy information to lists
        	else
        	{
        		if(u.getType() == baseType)
        		{
        			enemyBases.add(u.getID());
        		}
        	}
        	
        	
        	//Unit Actions
        	Macro(player);
        	if( u.getPlayer() == player)
        	{
        		
        		Micro(u); //
        	}
        	
        }
        
        //Cleanup Maps
        LinkedList<Long> removalItems = new LinkedList<Long>();
        for (Long key : commandMap.keySet())
        {
        	if(pgs.getUnit(key) == null)
        	{
        		removalItems.add(key);
        	}
        }
        for (Long i : removalItems)
        {
        	commandMap.remove(i);
        }
        playerBases.clear();
        enemyBases.clear();
        
        return translateActions(player, gs);
        
    }
    
    @Override
    public List<ParameterSpecification> getParameters() 
    {
        return new ArrayList<>();
    }
    
    //--- Custom Functions    

    private void Macro(int player)
    {
    	int MiningWorkers = 0;
    	int availibleResourcesBlocks = 0;
    	int numBarracks = 0;
    	int resources = p.getResources();
    	float WinChance = 0;
    	List<Integer> reservedPositions = new LinkedList<Integer>();
    	for (Long i : playerBases)
    	{
    		Unit PB = pgs.getUnit(i);
    		if (PB != null)
    		{
    			for(int y = -RESOURCE_CHECK + PB.getY(); y <= RESOURCE_CHECK + PB.getY(); y++)	
    			{
    				for(int x = -RESOURCE_CHECK + PB.getX(); x <= RESOURCE_CHECK + PB.getX(); x++)	
    				{
    					try
    					{
    						if (pgs.getUnitAt(x, y).getType() == resourceType)
    						{
    							availibleResourcesBlocks++;
    						}
    					}
    					catch(NullPointerException e) {}
    				}
    			}
    		}
    		else
    		{
    			playerBases.remove(i);
    		}
    	}
        for(Unit u:pgs.getUnits()) 
        {
        	if(u.getType() == workerType && MiningWorkers <= 2 && availibleResourcesBlocks > 0 && playerBases.size() > 0)
        	{	
        		MiningWorkers++;
        		commandMap.put(u.getID(), new Commands(attachedCommandEnum.farm));
        	}
        	else if (numBarracks == 0 && u.getType() == workerType && resources >= barracksType.cost) 
        	{
                // build a barracks:
        		commandMap.put(u.getID(), new Commands(attachedCommandEnum.Macro));
                //buildIfNotAlreadyBuilding(u,barracksType,playerBase.getX(),playerBase.getY()-2,reservedPositions,p,pgs);
                resources -= barracksType.cost;
            }
        	
        	else if(u.getType() == baseType && (resources <= barracksType.cost || resources >= barracksType.cost+1))
        	{
        		commandMap.put(u.getID(), new Commands(attachedCommandEnum.CreateWorker));
        	}
        	
        	else /*if(u.getType() == workerType) */
        	{
        		commandMap.put(u.getID(), new Commands(attachedCommandEnum.attack, calcClosestEnemy(u)));
        	}
        }
    }
   

	private void Micro(Unit u)
    {
		
    	try
    	{
    		Commands c = commandMap.get(u.getID());
    	    if(doThis(u,c) != 0)
    	    {
    	    	throw new IllegalArgumentException();
    	    }

    	}
    	catch(NullPointerException e)
    	{
    		System.out.println(u);
    	}
    	
    }
	public int doThis(Unit u, Commands atCommand)
	{
		switch (atCommand.attachedCommand)
		{
			case Macro:
				break;
			case farm:
				farm(u);
				break;
			case meleeAttackUnit:
				meleeAttackUnit(u, atCommand.enemyUnit);
				break;
			case rangedAttackUnit:
				rangedAttackUnit(u, atCommand.enemyUnit);
				break;
			case attackBase:
				attack(u,atCommand.enemyUnit);
				break;
			case attack:
				if (atCommand.enemyUnit != null) attack(u, atCommand.enemyUnit);	
				break;
			case defend:
				defend(u, atCommand.enemyUnit);
				break;
			case CreateWorker:
				if (p.getResources()>=workerType.cost) train(u, workerType);
				break;
			case CreateLight:
				if (p.getResources()>=lightType.cost) train(u, lightType);
				break;
			case CreateHeavy:
				if (p.getResources()>=heavyType.cost) train(u, heavyType);
				break;
			case CreateRanged:
				if (p.getResources()>=rangedType.cost) train(u, rangedType);
				break;
			
			default:
				return -1;
		}
		return 0;
	}
	private void farm(Unit u)
	{
		harvest(u, calcClosestType(u,resourceType,false),calcClosestType(u,baseType,true));
	}
	private void meleeAttackUnit(Unit u, Unit enemy)
	{
		
	}
	private void rangedAttackUnit(Unit u, Unit enemy)
	{
		
	}
	private void defend(Unit u, Unit Base)
	{
		
	}
	private Unit calcClosestEnemy(Unit u)
	{
		int distance = -1;
		Unit closest = null;
		for(Unit enemy: pgs.getUnits())
		{
			if (enemy.getPlayer() != u.getPlayer() && enemy.getType() != resourceType)
			{
				int d = Math.abs(enemy.getX() - u.getX()) + Math.abs(enemy.getY() - u.getY());
                if (closest == null || d < distance) 
                {
                	distance = d;
                	closest = enemy;
                }
			}
		}
		
		return closest;
	}
	private Unit calcClosestType(Unit u, UnitType type, boolean FindAlly)
	{
		int distance = -1;
		Unit closest = null;
		for(Unit u2: pgs.getUnits())
		{
			if ((!(u2.getPlayer() == u.getPlayer()^FindAlly)) && u2.getType() == type)
			{
				int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closest == null || d < distance) 
                {
                	distance = d;
                	closest = u2;
                }
			}

		}
		
		return closest;
	}
    
    
    //--- Custom Functions
}
