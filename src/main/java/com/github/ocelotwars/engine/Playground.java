package com.github.ocelotwars.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Playground {

	private Dimension dimension;
	private Tile[][] tiles;
	private Map<Integer, Unit> units;
	private List<Headquarter> headquarters;

	public Playground(Dimension dimension) {
		this.dimension = dimension;
		this.tiles = initTiles(dimension);
		this.units = new HashMap<>();
		this.headquarters = new ArrayList<>();
	}

	private static Tile[][] initTiles(Dimension dimension) {
		Tile[][] tiles = new Tile[dimension.width][dimension.height];
		for (int x = 0; x < tiles.length; x++) {
			for (int y = 0; y < tiles[x].length; y++) {
				tiles[x][y] = new Tile(new Position(x, y));
			}
		}
		return tiles;
	}

	public void putUnit(Unit unit, Position position) {
		Tile tile = getTileAt(position);
		tile.addUnit(unit);
		units.put(unit.getId(), unit);
	}

    public void putHeadquarter(Headquarter hq, Position position) {
        Tile tile = getTileAt(position);
        tile.addHeadquarter(hq);
        headquarters.add(hq);
    }

	public void putResource(int resources, Position position) {
		Tile tile = getTileAt(position);
		tile.setResources(resources);
	}

	public Unit getUnit(Player player, int unitId) {
		Unit unit = units.get(Integer.valueOf(unitId));
		if (unit == null) {
		    throw NoSuchAssetException.forUnit(unitId);
		}
		if (!player.equals(unit.getOwner())) {
		    throw NotUnitOwnerException.forPlayerAndUnit(player, unitId);
		}
        return unit;
	}

	public Tile getTileAt(Position targetPosition) {
		return tiles[targetPosition.x][targetPosition.y];
	}

	public Position shift(Position position, Direction direction) {
		return direction.shift(position).normalize(dimension);
	}

    public Headquarter getHeadQuarter(Player player) {
        return headquarters.stream()
            .filter(hq -> player.equals(hq.getOwner()))
            .findFirst()
            .orElseThrow(()-> NoSuchAssetException.forHeadquarter(player));
    }

}
