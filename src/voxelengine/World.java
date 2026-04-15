package voxelengine;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;

public class World {
    HashMap<ChunkPos, Chunk> chunks;
    int renderDistance;
    
    public World(int renderDistance) {
        this.chunks = new HashMap<>();
        this.renderDistance = renderDistance;
    }
    
    public void generateChunk(ChunkPos pos) {
        Chunk chunk = new Chunk(pos);

        for (int x = 0; x < Chunk.SIZE_X; x++) {
            for (int z = 0; z < Chunk.SIZE_Z; z++) {
                int worldX = pos.x * Chunk.SIZE_X + x;
                int worldZ = pos.z * Chunk.SIZE_Z + z;

                double nx = worldX * 0.05;
                double nz = worldZ * 0.05;

                double height = 0;
                height += PerlinNoise.noise(nx, 0, nz) * 20;
                height += PerlinNoise.noise(nx * 2, 0, nz * 2) * 10;
                height += PerlinNoise.noise(nx * 4, 0, nz * 4) * 5;
                height += 32;

                for (int y = 0; y < (int) height && y < Chunk.SIZE_Y; y++) {
                    chunk.data.set(x, y, z, (byte) 1);
                }
            }
        }
        
        // Mark neighbors dirty so they remesh with new boundary data
        ChunkPos[] neighbors = {
            new ChunkPos(pos.x - 1, pos.z),
            new ChunkPos(pos.x + 1, pos.z),
            new ChunkPos(pos.x, pos.z - 1),
            new ChunkPos(pos.x, pos.z + 1),
        };
        
        for (ChunkPos n : neighbors) {
            Chunk neighbor = chunks.get(n);
            if (neighbor != null) {
                neighbor.dirty = true;
            }
        }

        chunks.put(pos, chunk);
    }
    
    public void meshChunk(Chunk chunk) {
        chunk.mesh = ChunkMesher.buildMesh(chunk.data, this, chunk.pos);
    }
    
    public void update(float camX, float camZ) {
        int camChunkX = (int) Math.floor(camX / Chunk.SIZE_X);
        int camChunkZ = (int) Math.floor(camZ / Chunk.SIZE_Z);

        int chunksProcessed = 0;
        int maxPerFrame = 2;

        // Pass 1: Generate chunks in range
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                ChunkPos pos = new ChunkPos(camChunkX + dx, camChunkZ + dz);
                if (!chunks.containsKey(pos)) {
                    if (chunksProcessed >= maxPerFrame) {
                        continue;
                    }
                    generateChunk(pos);
                    chunksProcessed++;
                }
            }
        }

        // Pass 2: Mesh dirty chunks
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                ChunkPos pos = new ChunkPos(camChunkX + dx, camChunkZ + dz);
                Chunk chunk = chunks.get(pos);
                if (chunk != null && chunk.dirty) {
                    if (chunksProcessed >= maxPerFrame) {
                        continue;
                    }
                    if (chunk.mesh != null) {
                        chunk.mesh.cleanup();
                    }
                    chunk.mesh = ChunkMesher.buildMesh(chunk.data, World.this, chunk.pos);
                    chunk.dirty = false;
                    chunksProcessed++;
                }
            }
        }

        // Unload chunks too far away
        ArrayList<ChunkPos> toRemove = new ArrayList<>();
        for (ChunkPos pos : chunks.keySet()) {
            if (Math.abs(pos.x - camChunkX) > renderDistance + 2
                    || Math.abs(pos.z - camChunkZ) > renderDistance + 2) {
                toRemove.add(pos);
            }
        }
        
        for (ChunkPos pos : toRemove) {
            Chunk chunk = chunks.get(pos);
            if (chunk.mesh != null) {
                chunk.mesh.cleanup();
            }
            chunks.remove(pos);
        }
    }
    
    public Collection<Chunk> getVisibleChunks() {
        return chunks.values();
    }
    
    public byte getBlock(int worldX, int worldY, int worldZ) {
        if (worldY < 0 || worldY >= Chunk.SIZE_Y) {
            return 0;
        }
        int cx = (int) Math.floor((double) worldX / Chunk.SIZE_X);
        int cz = (int) Math.floor((double) worldZ / Chunk.SIZE_Z);
        ChunkPos pos = new ChunkPos(cx, cz);
        Chunk chunk = chunks.get(pos);
        if (chunk == null) {
            return 0;
        }
        int lx = worldX - cx * Chunk.SIZE_X;
        int lz = worldZ - cz * Chunk.SIZE_Z;
        return chunk.data.get(lx, worldY, lz);
    }
}
