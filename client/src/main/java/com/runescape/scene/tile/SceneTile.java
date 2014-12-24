package com.runescape.scene.tile;

import com.runescape.collection.Node;
import com.runescape.scene.CameraAngle;
import com.runescape.scene.SceneSpawnRequest;

public class SceneTile extends Node {

	public int plane;
	public int x;
	public int y;
	public GenericTile genericTile;
	public ComplexTile complexTile;
	public Wall wall;
	public WallDecoration wallDecoration;
	public FloorDecoration floorDecoration;
	public CameraAngle cameraAngle;
	public int sceneSpawnRequestCount;
	public SceneSpawnRequest[] sceneSpawnRequests = new SceneSpawnRequest[5];
	public int[] anIntArray1330 = new int[5];
	public int anInt1331;
	public int anInt1332;
	public boolean aBoolean1333;
	public boolean aBoolean1334;
	public boolean aBoolean1335;
	public int anInt1336;
	public int anInt1337;
	public int anInt1338;
	public int anInt1339;
	public SceneTile sceneTile;

	public SceneTile(int plane, int x, int y) {
		this.plane = plane;
		this.x = x;
		this.y = y;
	}
}
