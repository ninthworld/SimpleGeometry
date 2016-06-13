package org.ninthworld.simplegeometry.renderers;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.ninthworld.simplegeometry.chunk.ChunkEntity;
import org.ninthworld.simplegeometry.helper.MatrixHelper;
import org.ninthworld.simplegeometry.models.RawModel;
import org.ninthworld.simplegeometry.shaders.TerrainShader;

import java.util.HashMap;

/**
 * Created by NinthWorld on 6/6/2016.
 */
public class TerrainRenderer {

    private TerrainShader shader;

    public HashMap<String, ChunkEntity> chunks;

    public TerrainRenderer(Loader loader, TerrainShader shader, Matrix4f projectionMatrix) {
        this.shader = shader;

        chunks = new HashMap<>();
        int radius = 8;
        for(int x=-radius; x<radius; x++){
            for(int y=-1; y<2; y++){
                for(int z=-radius; z<radius; z++){
                    chunks.put(ChunkEntity.chunkVecString(x, y, z), new ChunkEntity(loader, new Vector3f(x, y, z)));
                }
            }
        }

        for(ChunkEntity chunkEntity : chunks.values()){
            Vector3f pos = chunkEntity.getPosition();
            ChunkEntity[] adjChunks = new ChunkEntity[]{
                    chunks.get(ChunkEntity.chunkVecString(pos.x, pos.y-1, pos.z)),
                    chunks.get(ChunkEntity.chunkVecString(pos.x, pos.y+1, pos.z)),
                    chunks.get(ChunkEntity.chunkVecString(pos.x, pos.y, pos.z-1)),
                    chunks.get(ChunkEntity.chunkVecString(pos.x, pos.y, pos.z+1)),
                    chunks.get(ChunkEntity.chunkVecString(pos.x-1, pos.y, pos.z)),
                    chunks.get(ChunkEntity.chunkVecString(pos.x+1, pos.y, pos.z))
            };
            chunkEntity.generateModel(loader, adjChunks);
        }

        shader.start();
        shader.loadProjectionMatrix(projectionMatrix);
        shader.stop();
    }

    public void render(Matrix4f toShadowSpace) {
        if(toShadowSpace != null)
            shader.loadToShadowSpaceMatrix(toShadowSpace);

        for(ChunkEntity chunkEntity : chunks.values()) {
            prepareModel(chunkEntity.getRawModel());
            prepareInstance(chunkEntity);
            GL11.glDrawElements(GL11.GL_TRIANGLES, chunkEntity.getRawModel().getVertexCount(), GL11.GL_UNSIGNED_INT, 0);
        }
        unbindModel();
    }

    private void prepareModel(RawModel rawModel) {
        GL30.glBindVertexArray(rawModel.getVaoID());
        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);
//        ModelTexture texture = model.getTexture();
//        shader.loadShineVariables(texture.getShineDamper(), texture.getReflectivity());
//        GL13.glActiveTexture(GL13.GL_TEXTURE0);
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, model.getTexture().getID());
    }

    private void unbindModel() {
        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);
        GL30.glBindVertexArray(0);
    }

    private void prepareInstance(ChunkEntity entity) {
        Matrix4f transformationMatrix = MatrixHelper.createTransformationMatrix(new Vector3f(entity.getPosition().getX()*ChunkEntity.CHUNK_SIZE, entity.getPosition().getY()*ChunkEntity.CHUNK_SIZE, entity.getPosition().getZ()*ChunkEntity.CHUNK_SIZE), 0, 0, 0, 1);
        shader.loadTransformationMatrix(transformationMatrix);
    }
}
