/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.extent.clipboard;

import static com.google.common.base.Preconditions.checkNotNull;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.clipboard.DelegateClipboard;
import com.boydti.fawe.object.clipboard.DiskOptimizedClipboard;
import com.boydti.fawe.object.clipboard.LinearClipboard;
import com.boydti.fawe.object.clipboard.MemoryOptimizedClipboard;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import jdk.vm.ci.meta.Local;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Stores block data as a multi-dimensional array of {@link BlockState}s and
 * other data as lists or maps.
 */
public class BlockArrayClipboard extends DelegateClipboard implements Clipboard, Closeable {

    private Region region;
    private BlockVector3 origin;

    public BlockArrayClipboard(Region region) {
        this(region, UUID.randomUUID());
    }

    /**
     * Create a new instance.
     *
     * <p>The origin will be placed at the region's lowest minimum point.</p>
     *
     * @param region the bounding region
     */
    public BlockArrayClipboard(Region region, UUID clipboardId) {
        this(region, Clipboard.create(region.getDimensions(), clipboardId));
        checkNotNull(region);
        this.region = region.clone();
        this.origin = region.getMinimumPoint();
    }

    public BlockArrayClipboard(Region region, Clipboard clipboard) {
        super(clipboard);
        checkNotNull(region);
        this.region = region.clone();
        this.origin = region.getMinimumPoint();
    }

    @Override
    public void close() throws IOException {
        if (getParent() instanceof Closeable) {
            ((Closeable) getParent()).close();
        }
    }

    @Override
    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    @Override
    public BlockVector3 getOrigin() {
        return origin;
    }

    @Override
    public void setOrigin(BlockVector3 origin) {
        this.origin = origin;
        getParent().setOrigin(origin.subtract(region.getMinimumPoint()));
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return region.getMinimumPoint();
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return region.getMaximumPoint();
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        if (region.contains(position)) {
            int x = position.getBlockX()- origin.getX();
            int y = position.getBlockY()- origin.getY();
            int z = position.getBlockZ()- origin.getZ();
            return getParent().getBlock(x, y, z);
        }

        return BlockTypes.AIR.getDefaultState();
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        if(region.contains(position)) {
            int x = position.getBlockX()- origin.getX();
            int y = position.getBlockY()- origin.getY();
            int z = position.getBlockZ()- origin.getZ();
            return getParent().getFullBlock(x, y, z);
        }
        return BlockTypes.AIR.getDefaultState().toBaseBlock();
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block) throws WorldEditException {
        if (region.contains(position)) {
            final int x = position.getBlockX();
            final int y = position.getBlockY();
            final int z = position.getBlockZ();
            return setBlock(x, y, z, block);
        }
        return false;
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        x -= origin.getX();
        y -= origin.getY();
        z -= origin.getZ();
        return getParent().setTile(x, y, z, tag);
    }

    public boolean setTile(BlockVector3 position, CompoundTag tag) {
        return setTile(position.getX(), position.getY(), position.getZ(), tag);
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) throws WorldEditException {
        x -= origin.getX();
        y -= origin.getY();
        z -= origin.getZ();
        return getParent().setBlock(x, y, z, block);
    }

    @Override
    public boolean hasBiomes() {
        return getParent().hasBiomes();
    }

    @Override
    public BiomeType getBiome(BlockVector2 position) {
        BlockVector2 v = position.subtract(region.getMinimumPoint().toBlockVector2());
        return getParent().getBiomeType(v.getX(), v.getZ());
    }

    @Override
    public boolean setBiome(BlockVector2 position, BiomeType biome) {
        int x = position.getBlockX()- origin.getX();
        int z = position.getBlockZ()- origin.getZ();
        return getParent().setBiome(x, 0, z, biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return parent.setBiome(x, y, z, biome);
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        return parent.getEntities(region);
    }

    @Override
    @Nullable
    public Entity createEntity(Location location, BaseEntity entity) {
        return parent.createEntity(location, entity);
    }

    @Override
    @Nullable
    public void removeEntity(int x, int y, int z, UUID uuid) {
        parent.removeEntity(x, y, z, uuid);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return parent.getBlock(x, y, z);
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return parent.getFullBlock(x, y, z);
    }

    @Override
    public BiomeType getBiomeType(int x, int z) {
        return parent.getBiomeType(x, z);
    }

    /**
     * Stores entity data.
     */
    public static class ClipboardEntity implements Entity {
        private final BaseEntity entity;
        private final Clipboard clipboard;
        private final double x, y, z;
        private final float yaw, pitch;

        public ClipboardEntity(Location loc, BaseEntity entity) {
            this((Clipboard) loc.getExtent(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getYaw(), loc.getPitch(), entity);
        }

        public ClipboardEntity(Clipboard clipboard, double x, double y, double z, float yaw, float pitch, BaseEntity entity) {
            checkNotNull(entity);
            checkNotNull(clipboard);
            this.clipboard = clipboard;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.entity = new BaseEntity(entity);
        }

        @Override
        public boolean remove() {
            clipboard.removeEntity(this);
            return true;
        }

        @Nullable
        @Override
        public <T> T getFacet(Class<? extends T> cls) {
            return null;
        }

        /**
         * Get the entity state. This is not a copy.
         *
         * @return the entity
         */
        BaseEntity getEntity() {
            return entity;
        }

        @Override
        public BaseEntity getState() {
            return new BaseEntity(entity);
        }

        @Override
        public Location getLocation() {
            return new Location(clipboard, x, y, z, yaw, pitch);
        }

        @Override
        public Extent getExtent() {
            return clipboard;
        }

        @Override
        public boolean setLocation(Location loc) {
            clipboard.removeEntity(this);
            Entity result = clipboard.createEntity(loc, entity);
            return result != null;
        }
    }
}
