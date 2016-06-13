package org.ninthworld.simplegeometry.chunk;

import org.lwjgl.Sys;
import org.lwjgl.util.vector.Vector3f;
import org.ninthworld.simplegeometry.helper.SimplexNoise;
import org.ninthworld.simplegeometry.helper.VoxelHelper;
import org.ninthworld.simplegeometry.models.RawModel;
import org.ninthworld.simplegeometry.renderers.Loader;

import java.util.Random;

/**
 * Created by NinthWorld on 6/7/2016.
 */
public class ChunkEntity {

    public static final int CHUNK_SIZE = 16;
    public static final Vector3f[] materials = {
            new Vector3f(168/255f, 240/255f, 93/255f), // Grass
            new Vector3f(83/255f, 66/255f, 48/255f), // Dirt
            new Vector3f(97/255f, 101/255f, 101/255f) // Stone
    };

    private RawModel model;
    private Vector3f position;
    private int[][][] voxelData;

    public ChunkEntity(Loader loader, Vector3f position) {
        this.position = position;
        this.voxelData = new int[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];
        generateChunk(12345L);
    }

    public void generateModel(Loader loader, ChunkEntity[] adjChunks){
        VoxelHelper.VoxelFaces[][][] voxelFaces = VoxelHelper.calculateFaces(voxelData, false, adjChunks);
        VoxelHelper.Faces[] faces = VoxelHelper.greedyMesh(voxelFaces, voxelData, materials);

        this.model = VoxelHelper.generateRawModel(loader, faces);
    }

    public int getVoxelData(int x, int y, int z){
        return voxelData[x][y][z];
    }

    public RawModel getRawModel() {
        return model;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void generateChunk(long seed){
        SimplexNoise noise1 = new SimplexNoise(seed);
        SimplexNoise noise2 = new SimplexNoise(seed + 1);

        float freq1 = 0.03f;
        float amp1 = 2f;

        float freq2 = 0.01f;
        float amp2 = 6f;

        for(int i=0; i<voxelData.length; i++){
            for(int k=0; k<voxelData[i][0].length; k++) {
                float height1 = noise1.noise((i + position.getX()*CHUNK_SIZE)*freq1, (k + position.getZ()*CHUNK_SIZE)*freq1)*amp1;
                float height2 = noise2.noise((i + position.getX()*CHUNK_SIZE)*freq2, (k + position.getZ()*CHUNK_SIZE)*freq2)*amp2;

                float height3 = (height1 + height2)/2f;

                for(int j=0; j<voxelData[i].length; j++) {
                    if(j + position.getY()*CHUNK_SIZE <= height3){
                        voxelData[i][j][k] = 1;
                    }
                }
            }
        }
    }

    public static String chunkVecString(float x, float y, float z){
        String str = Integer.toString((int)x) + "," + Integer.toString((int)y) + "," + Integer.toString((int)z);
        // System.out.println(str);
        return str;
    }

}