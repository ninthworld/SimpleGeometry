package org.ninthworld.simplegeometry.helper;

import org.lwjgl.util.vector.Vector3f;
import org.ninthworld.simplegeometry.chunk.ChunkEntity;
import org.ninthworld.simplegeometry.entities.Entity;
import org.ninthworld.simplegeometry.models.Model;
import org.ninthworld.simplegeometry.models.RawModel;
import org.ninthworld.simplegeometry.renderers.Loader;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by NinthWorld on 6/9/2016.
 */
public class VoxelHelper {

    public static Entity load(Loader loader, BufferedImage img){
        List<Vector3f> colors = new ArrayList<>();
        int maxX = 0, maxY = 0, maxZ = 0;

        int[][] imgData = new int[img.getWidth()][img.getHeight()-1];
        for(int x=0; x<img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                Color c = new Color(img.getRGB(x, y), true);
                if (x == 0 && y == 0) {
                    maxX = c.getRed();
                    maxY = c.getGreen();
                    maxZ = c.getBlue();
                }

                if (y > 0 && c.getAlpha() > 0) {
                    Vector3f color = new Vector3f(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f);
                    int index = -1;
                    for (Vector3f col : colors) {
                        if (col.x == color.x && col.y == color.y && col.z == color.z) {
                            index = colors.indexOf(col);
                        }
                    }
                    if (index == -1) {
                        colors.add(color);
                        index = colors.indexOf(color);
                    }
                    imgData[x][y - 1] = index+1;
                }
            }
        }

        int[][][] data = new int[maxX][maxY][maxZ];
        for(int z=0; z<data[0][0].length; z++){
            for(int x=0; x<data.length; x++){
                for(int y=0; y<data[0].length; y++){
                    data[x][data[0].length-y-1][z] = imgData[x][y + z*maxY];
                }
            }
        }

        Vector3f[] materials = new Vector3f[colors.size()];
        for(int i=0; i<materials.length; i++){
            materials[i] = colors.get(i);
        }

        VoxelFaces[][][] voxelFaces = VoxelHelper.calculateFaces(data, true);
        Faces[] faces = VoxelHelper.greedyMesh(voxelFaces, data, materials);

        Model model = new Model(generateRawModel(loader, faces));
        Entity entity = new Entity(model, new Vector3f(0, 0, 0), 0, 0, 0, 0.5f);

        return entity;
    }

    public static Entity load(Loader loader, File file) throws IOException {
        List<Vector3f> colors = new ArrayList<>();
        int colorCount = -1;

        int[][][] data = null;
        int xCount = -1;
        int yCount = -1;

        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while((line = br.readLine()) != null){
            line = line.replaceAll(" ", "");
            if(!line.startsWith("#")){
                if(colorCount == -1){
                    colorCount = Integer.parseInt(line);
                }else if(colorCount > 0){
                    String[] split = line.split(",");
                    colors.add(new Vector3f(Integer.parseInt(split[0])/255f, Integer.parseInt(split[1])/255f, Integer.parseInt(split[2])/255f));
                    colorCount--;
                }else if(data == null){
                    String[] split = line.split(",");
                    int x = Integer.parseInt(split[0]);
                    int y = Integer.parseInt(split[1]);
                    int z = Integer.parseInt(split[2]);
                    data = new int[x][y][z];
                    xCount = z;
                    yCount = y;
                }else if(xCount > 0){
                    if(yCount <= 0){
                        yCount = data[0].length;
                        xCount--;
                    }
                    if(yCount > 0){
                        String[] split = line.split(",");
                        for(int i=0; i<split.length; i++){
                            data[data.length-xCount][data[0].length-yCount][i] = Integer.parseInt(split[i]);
                        }
                        yCount--;
                    }
                }
            }
        }
        br.close();

        Vector3f[] materials = new Vector3f[colors.size()];
        for(int i=0; i<materials.length; i++){
            materials[i] = colors.get(i);
        }

        VoxelFaces[][][] voxelFaces = VoxelHelper.calculateFaces(data, true);
        Faces[] faces = VoxelHelper.greedyMesh(voxelFaces, data, materials);

        Model model = new Model(generateRawModel(loader, faces));
        Entity entity = new Entity(model, new Vector3f(0, 0, 0), 0, 0, 0, 0.2f);

        return entity;
    }

    public static VoxelFaces[][][] calculateFaces(int[][][] voxels, boolean showOutside){
        return calculateFaces(voxels, showOutside, new ChunkEntity[6]);
    }

    public static VoxelFaces[][][] calculateFaces(int[][][] voxels, boolean showOutside, ChunkEntity[] adjChunks){
        VoxelFaces[][][] outputFaces = new VoxelFaces[voxels.length][voxels[0].length][voxels[0][0].length];

        for(int i=0; i<voxels.length; i++) {
            for (int j = 0; j < voxels[i].length; j++) {
                for (int k = 0; k < voxels[i][j].length; k++) {
                    int material = voxels[i][j][k];
                    outputFaces[i][j][k] = new VoxelFaces();
                    if(material > 0) {
                        boolean[] faces = outputFaces[i][j][k].faces;
                        faces[0] = (j < voxels[i].length-1 && voxels[i][j+1][k] == 0) || (j >= voxels[i].length-1 && (showOutside || (adjChunks[1] != null && adjChunks[1].getVoxelData(i, 0, k) == 0)));
                        faces[1] = (j > 0 && voxels[i][j-1][k] == 0) || (j == 0 && (showOutside || (adjChunks[0] != null && adjChunks[0].getVoxelData(i, ChunkEntity.CHUNK_SIZE-1, k) == 0)));
                        faces[2] = (k < voxels[i][j].length-1 && voxels[i][j][k+1] == 0) || (k >= voxels[i][j].length-1 && (showOutside || (adjChunks[3] != null && adjChunks[3].getVoxelData(i, j, 0) == 0)));
                        faces[3] = (k > 0 && voxels[i][j][k-1] == 0) || (k == 0 && (showOutside || (adjChunks[2] != null && adjChunks[2].getVoxelData(i, j, ChunkEntity.CHUNK_SIZE-1) == 0)));
                        faces[4] = (i < voxels.length-1 && voxels[i+1][j][k] == 0) || (i >= voxels.length-1 && (showOutside || (adjChunks[5] != null && adjChunks[5].getVoxelData(0, j, k) == 0)));
                        faces[5] = (i > 0 && voxels[i-1][j][k] == 0) || (i == 0 && (showOutside || (adjChunks[4] != null && adjChunks[4].getVoxelData(ChunkEntity.CHUNK_SIZE-1, j, k) == 0)));
                    }
                }
            }
        }

        return outputFaces;
    }

    public static Faces[] greedyMesh(VoxelFaces[][][] voxelFaces, int[][][] voxels, Vector3f[] materials){
        Faces[] allFaces = new Faces[6];

        // Top and Bottom Faces
        for(int currentFace = 0; currentFace < 2; currentFace++) {
            allFaces[currentFace] = new Faces();
            for (int y = 0; y < voxelFaces[0].length; y++) {
                for (int x = 0; x < voxelFaces.length; x++) {
                    for (int z = 0; z < voxelFaces[0][0].length; z++) {
                        if (voxelFaces[x][y][z].faces[currentFace]) {
                            int material = voxels[x][y][z];
                            int maxWidth = 0;
                            while (z + maxWidth + 1 < voxelFaces[x][y].length && voxelFaces[x][y][z + maxWidth + 1].faces[currentFace] && voxels[x][y][z + maxWidth + 1] == material) {
                                maxWidth++;
                            }

                            int height = 0;
                            loop:
                            while (x + height + 1 < voxelFaces.length) {
                                for (int width = 0; width <= maxWidth; width++) {
                                    if (!voxelFaces[x + height + 1][y][z + width].faces[currentFace] || voxels[x + height + 1][y][z + width] != material) {
                                        break loop;
                                    }
                                }
                                height++;
                            }

                            for (int i = 0; i <= height; i++) {
                                for (int j = 0; j <= maxWidth; j++) {
                                    voxelFaces[x + i][y][z + j].faces[currentFace] = false;
                                }
                            }

                            allFaces[currentFace].faceList.add(new Face(new Vector3f(x, y, z), new Vector3f(x + height, y, z + maxWidth), materials[material - 1]));
                        }
                    }
                }
            }
        }

        // Front and Back Faces
        for(int currentFace = 2; currentFace < 4; currentFace++) {
            allFaces[currentFace] = new Faces();
            for (int z = 0; z < voxelFaces[0][0].length; z++) {
                for (int x = 0; x < voxelFaces.length; x++) {
                    for (int y = 0; y < voxelFaces[0].length; y++) {
                        if (voxelFaces[x][y][z].faces[currentFace]) {
                            int material = voxels[x][y][z];
                            int maxWidth = 0;
                            while (y + maxWidth + 1 < voxelFaces[x].length && voxelFaces[x][y + maxWidth + 1][z].faces[currentFace] && voxels[x][y + maxWidth + 1][z] == material) {
                                maxWidth++;
                            }

                            int height = 0;
                            loop:
                            while (x + height + 1 < voxelFaces.length) {
                                for (int width = 0; width <= maxWidth; width++) {
                                    if (!voxelFaces[x + height + 1][y + width][z].faces[currentFace] || voxels[x + height + 1][y + width][z] != material) {
                                        break loop;
                                    }
                                }
                                height++;
                            }

                            for (int i = 0; i <= height; i++) {
                                for (int j = 0; j <= maxWidth; j++) {
                                    voxelFaces[x + i][y + j][z].faces[currentFace] = false;
                                }
                            }

                            allFaces[currentFace].faceList.add(new Face(new Vector3f(x, y, z), new Vector3f(x + height, y + maxWidth, z), materials[material - 1]));
                        }
                    }
                }
            }
        }

        // Right and Left Faces
        for(int currentFace = 4; currentFace < 6; currentFace++) {
            allFaces[currentFace] = new Faces();
            for (int x = 0; x < voxelFaces.length; x++) {
                for (int z = 0; z < voxelFaces[0][0].length; z++) {
                    for (int y = 0; y < voxelFaces[0].length; y++) {
                        if (voxelFaces[x][y][z].faces[currentFace]) {
                            int material = voxels[x][y][z];
                            int maxWidth = 0;
                            while (y + maxWidth + 1 < voxelFaces[x].length && voxelFaces[x][y + maxWidth + 1][z].faces[currentFace] && voxels[x][y + maxWidth + 1][z] == material) {
                                maxWidth++;
                            }

                            int height = 0;
                            loop:
                            while (z + height + 1 < voxelFaces[0][0].length) {
                                for (int width = 0; width <= maxWidth; width++) {
                                    if (!voxelFaces[x][y + width][z + height + 1].faces[currentFace] || voxels[x][y + width][z + height + 1] != material) {
                                        break loop;
                                    }
                                }
                                height++;
                            }

                            for (int i = 0; i <= height; i++) {
                                for (int j = 0; j <= maxWidth; j++) {
                                    voxelFaces[x][y + j][z + i].faces[currentFace] = false;
                                }
                            }

                            allFaces[currentFace].faceList.add(new Face(new Vector3f(x, y, z), new Vector3f(x, y + maxWidth, z + height), materials[material - 1]));
                        }
                    }
                }
            }
        }

        return allFaces;
    }

    public static RawModel generateRawModel(Loader loader, Faces[] allFaces){
        List<Integer> indicesList = new ArrayList<>();
        List<Float> verticesList = new ArrayList<>();
        List<Float> colorsList = new ArrayList<>();
        List<Float> normalsList = new ArrayList<>();
        int offset = 0;

        // Top faces
        for(int i=0; i<allFaces[0].faceList.size(); i++){
            Face face = allFaces[0].faceList.get(i);
            float[] vertices = new float[]{
                    -0.5f + face.startVoxel.getX(), 0.5f + face.startVoxel.getY(), 0.5f + face.endVoxel.getZ(),
                    0.5f + face.endVoxel.getX(), 0.5f + face.startVoxel.getY(), 0.5f + face.endVoxel.getZ(),
                    0.5f + face.endVoxel.getX(), 0.5f + face.startVoxel.getY(), -0.5f + face.startVoxel.getZ(),
                    -0.5f + face.startVoxel.getX(), 0.5f + face.startVoxel.getY(), -0.5f + face.startVoxel.getZ()
            };
            float[] normals = new float[]{
                    0, 1, 0,
                    0, 1, 0,
                    0, 1, 0,
                    0, 1, 0
            };
            addIntegerArrayToList(indicesList, getIndices(offset));
            addFloatArrayToList(colorsList, getColors(face.color));
            addFloatArrayToList(verticesList, vertices);
            addFloatArrayToList(normalsList, normals);
            offset += 4;
        }

        // Bottom faces
        for(int i=0; i<allFaces[1].faceList.size(); i++){
            Face face = allFaces[1].faceList.get(i);
            float[] vertices = new float[]{
                    0.5f + face.endVoxel.getX(), -0.5f + face.startVoxel.getY(), -0.5f + face.startVoxel.getZ(),
                    0.5f + face.endVoxel.getX(), -0.5f + face.startVoxel.getY(), 0.5f + face.endVoxel.getZ(),
                    -0.5f + face.startVoxel.getX(), -0.5f + face.startVoxel.getY(), 0.5f + face.endVoxel.getZ(),
                    -0.5f + face.startVoxel.getX(), -0.5f + face.startVoxel.getY(), -0.5f + face.startVoxel.getZ()
            };
            float[] normals = new float[]{
                    0, -1, 0,
                    0, -1, 0,
                    0, -1, 0,
                    0, -1, 0
            };
            addIntegerArrayToList(indicesList, getIndices(offset));
            addFloatArrayToList(colorsList, getColors(face.color));
            addFloatArrayToList(verticesList, vertices);
            addFloatArrayToList(normalsList, normals);
            offset += 4;
        }

        // Front faces
        for(int i=0; i<allFaces[2].faceList.size(); i++){
            Face face = allFaces[2].faceList.get(i);
            float[] vertices = new float[]{
                    0.5f + face.endVoxel.getX(), -0.5f + face.startVoxel.getY(), 0.5f + face.startVoxel.getZ(),
                    0.5f + face.endVoxel.getX(), 0.5f + face.endVoxel.getY(), 0.5f + face.startVoxel.getZ(),
                    -0.5f + face.startVoxel.getX(), 0.5f + face.endVoxel.getY(), 0.5f + face.startVoxel.getZ(),
                    -0.5f + face.startVoxel.getX(), -0.5f + face.startVoxel.getY(), 0.5f + face.startVoxel.getZ()
            };
            float[] normals = new float[]{
                    0, 0, 1,
                    0, 0, 1,
                    0, 0, 1,
                    0, 0, 1
            };
            addIntegerArrayToList(indicesList, getIndices(offset));
            addFloatArrayToList(colorsList, getColors(face.color));
            addFloatArrayToList(verticesList, vertices);
            addFloatArrayToList(normalsList, normals);
            offset += 4;
        }

        // Back faces
        for(int i=0; i<allFaces[3].faceList.size(); i++){
            Face face = allFaces[3].faceList.get(i);
            float[] vertices = new float[]{
                    -0.5f + face.startVoxel.getX(), 0.5f + face.endVoxel.getY(), -0.5f + face.startVoxel.getZ(),
                    0.5f + face.endVoxel.getX(), 0.5f + face.endVoxel.getY(), -0.5f + face.startVoxel.getZ(),
                    0.5f + face.endVoxel.getX(), -0.5f + face.startVoxel.getY(), -0.5f + face.startVoxel.getZ(),
                    -0.5f + face.startVoxel.getX(), -0.5f + face.startVoxel.getY(), -0.5f + face.startVoxel.getZ()
            };
            float[] normals = new float[]{
                    0, 0, -1,
                    0, 0, -1,
                    0, 0, -1,
                    0, 0, -1
            };
            addIntegerArrayToList(indicesList, getIndices(offset));
            addFloatArrayToList(colorsList, getColors(face.color));
            addFloatArrayToList(verticesList, vertices);
            addFloatArrayToList(normalsList, normals);
            offset += 4;
        }

        // Right faces
        for(int i=0; i<allFaces[4].faceList.size(); i++){
            Face face = allFaces[4].faceList.get(i);
            float[] vertices = new float[]{
                    0.5f + face.startVoxel.getX(), 0.5f + face.endVoxel.getY(), -0.5f + face.startVoxel.getZ(),
                    0.5f + face.startVoxel.getX(), 0.5f + face.endVoxel.getY(), 0.5f + face.endVoxel.getZ(),
                    0.5f + face.startVoxel.getX(), -0.5f + face.startVoxel.getY(), 0.5f + face.endVoxel.getZ(),
                    0.5f + face.startVoxel.getX(), -0.5f + face.startVoxel.getY(), -0.5f + face.startVoxel.getZ()
            };
            float[] normals = new float[]{
                    1, 0, 0,
                    1, 0, 0,
                    1, 0, 0,
                    1, 0, 0
            };
            addIntegerArrayToList(indicesList, getIndices(offset));
            addFloatArrayToList(colorsList, getColors(face.color));
            addFloatArrayToList(verticesList, vertices);
            addFloatArrayToList(normalsList, normals);
            offset += 4;
        }

        // Left faces
        for(int i=0; i<allFaces[5].faceList.size(); i++){
            Face face = allFaces[5].faceList.get(i);
            float[] vertices = new float[]{
                    -0.5f + face.startVoxel.getX(), -0.5f + face.startVoxel.getY(), 0.5f + face.endVoxel.getZ(),
                    -0.5f + face.startVoxel.getX(), 0.5f + face.endVoxel.getY(), 0.5f + face.endVoxel.getZ(),
                    -0.5f + face.startVoxel.getX(), 0.5f + face.endVoxel.getY(), -0.5f + face.startVoxel.getZ(),
                    -0.5f + face.startVoxel.getX(), -0.5f + face.startVoxel.getY(), -0.5f + face.startVoxel.getZ()
            };
            float[] normals = new float[]{
                    -1, 0, 0,
                    -1, 0, 0,
                    -1, 0, 0,
                    -1, 0, 0
            };
            addIntegerArrayToList(indicesList, getIndices(offset));
            addFloatArrayToList(colorsList, getColors(face.color));
            addFloatArrayToList(verticesList, vertices);
            addFloatArrayToList(normalsList, normals);
            offset += 4;
        }

        int[] indicesArray = new int[indicesList.size()];
        for(int i=0; i<indicesArray.length; i++){
            indicesArray[i] = indicesList.get(i);
        }

        float[] verticesArray = new float[verticesList.size()];
        for(int i=0; i<verticesArray.length; i++){
            verticesArray[i] = verticesList.get(i);
        }

        float[] colorsArray = new float[colorsList.size()];
        for(int i=0; i<colorsArray.length; i++){
            colorsArray[i] = colorsList.get(i);
        }

        float[] normalsArray = new float[normalsList.size()];
        for(int i=0; i<normalsArray.length; i++){
            normalsArray[i] = normalsList.get(i);
        }

        return loader.loadToVao(verticesArray, colorsArray, normalsArray, indicesArray);
    }

    private static float[] getColors(Vector3f color){
        return new float[]{
                color.x, color.y, color.z,
                color.x, color.y, color.z,
                color.x, color.y, color.z,
                color.x, color.y, color.z
        };
    }

    private static int[] getIndices(int offset){
        return new int[]{
                offset, offset+1, offset+2,
                offset+2, offset+3, offset,
        };
    }

    private static void addFloatArrayToList(List<Float> list, float[] array){
        for(float val : array){
            list.add(val);
        }
    }

    private static void addIntegerArrayToList(List<Integer> list, int[] array){
        for(int val : array){
            list.add(val);
        }
    }

    public static class Faces {
        public List<Face> faceList;
        public Faces(){
            this.faceList = new ArrayList<>();
        }
    }

    public static class Face {
        public Vector3f startVoxel;
        public Vector3f endVoxel;
        public Vector3f color;
        public Face(Vector3f startVoxel, Vector3f endVoxel, Vector3f color){
            this.startVoxel = startVoxel;
            this.endVoxel = endVoxel;
            this.color = color;
        }
    }

    public static class VoxelFaces {
        public boolean[] faces;
        public VoxelFaces(){
            this.faces = new boolean[6];
        }
    }

}
