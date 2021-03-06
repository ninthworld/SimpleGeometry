package org.ninthworld.simplegeometry.shadows;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.ninthworld.simplegeometry.chunk.ChunkEntity;
import org.ninthworld.simplegeometry.entities.Entity;
import org.ninthworld.simplegeometry.helper.MatrixHelper;
import org.ninthworld.simplegeometry.models.Model;
import org.ninthworld.simplegeometry.models.RawModel;
import org.ninthworld.simplegeometry.renderers.TerrainRenderer;

import java.util.List;
import java.util.Map;

public class ShadowMapTerrainRenderer {

	private Matrix4f projectionViewMatrix;
	private ShadowShader shader;
	private TerrainRenderer terrainRenderer;

	/**
	 * @param shader
	 *            - the simple shader program being used for the shadow render
	 *            pass.
	 * @param projectionViewMatrix
	 *            - the orthographic projection matrix multiplied by the light's
	 *            "view" matrix.
	 */
	protected ShadowMapTerrainRenderer(TerrainRenderer terrainRenderer, ShadowShader shader, Matrix4f projectionViewMatrix) {
		this.terrainRenderer = terrainRenderer;
		this.shader = shader;
		this.projectionViewMatrix = projectionViewMatrix;
	}

	protected void render() {
		for(ChunkEntity chunkEntity : terrainRenderer.chunks.values()) {
			bindModel(chunkEntity.getRawModel());
			prepareInstance(chunkEntity);
			GL11.glDrawElements(GL11.GL_TRIANGLES, chunkEntity.getRawModel().getVertexCount(), GL11.GL_UNSIGNED_INT, 0);
		}
		GL20.glDisableVertexAttribArray(0);
		GL30.glBindVertexArray(0);
	}

	/**
	 * Binds a raw model before rendering. Only the attribute 0 is enabled here
	 * because that is where the positions are stored in the VAO, and only the
	 * positions are required in the vertex shader.
	 * 
	 * @param rawModel
	 *            - the model to be bound.
	 */
	private void bindModel(RawModel rawModel) {
		GL30.glBindVertexArray(rawModel.getVaoID());
		GL20.glEnableVertexAttribArray(0);
	}

	/**
	 * Prepares an entity to be rendered. The model matrix is created in the
	 * usual way and then multiplied with the projection and view matrix (often
	 * in the past we've done this in the vertex shader) to create the
	 * mvp-matrix. This is then loaded to the vertex shader as a uniform.
	 * 
	 * @param entity
	 *            - the entity to be prepared for rendering.
	 */
	private void prepareInstance(ChunkEntity entity) {
		Matrix4f modelMatrix = MatrixHelper.createTransformationMatrix(new Vector3f(entity.getPosition().getX()* ChunkEntity.CHUNK_SIZE, entity.getPosition().getY()*ChunkEntity.CHUNK_SIZE, entity.getPosition().getZ()*ChunkEntity.CHUNK_SIZE), 0, 0, 0, 1);
		Matrix4f mvpMatrix = Matrix4f.mul(projectionViewMatrix, modelMatrix, null);
		shader.loadMvpMatrix(mvpMatrix);
	}

}
