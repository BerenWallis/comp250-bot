package bot;

import rts.units.Unit;

public class Commands {
	public enum attachedCommandEnum
	{
		farm,
		meleeAttackUnit,
		rangedAttackUnit,
		attackBase,
		attack,
		defend,
		CreateWorker,
		CreateLight,
		CreateHeavy,
		CreateRanged,
		Macro
	}
	attachedCommandEnum attachedCommand;
	Unit enemyUnit = null;
	public Commands(attachedCommandEnum c)
	{
		attachedCommand = c;
	}
	public Commands(attachedCommandEnum c, Unit e)
	{
		attachedCommand = c;
		enemyUnit = e;
	}
}