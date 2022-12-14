package net.minecraft.world.level;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistry;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.PlayerChunk;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.tags.ITagRegistry;
import net.minecraft.util.MathHelper;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingManager;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockFireAbstract;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ITickable;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.chunk.IChunkProvider;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.saveddata.maps.WorldMap;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.storage.WorldDataMutable;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.scores.Scoreboard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.protocol.game.PacketPlayOutWorldBorder;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.level.border.IWorldBorderListener;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.SpigotTimings; // Spigot
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CapturedBlockState;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.event.block.BlockPhysicsEvent;
// CraftBukkit end

public abstract class World implements GeneratorAccess, AutoCloseable {

    protected static final Logger LOGGER = LogManager.getLogger();
    public static final Codec<ResourceKey<World>> f = MinecraftKey.a.xmap(ResourceKey.b(IRegistry.L), ResourceKey::a);
    public static final ResourceKey<World> OVERWORLD = ResourceKey.a(IRegistry.L, new MinecraftKey("overworld"));
    public static final ResourceKey<World> THE_NETHER = ResourceKey.a(IRegistry.L, new MinecraftKey("the_nether"));
    public static final ResourceKey<World> THE_END = ResourceKey.a(IRegistry.L, new MinecraftKey("the_end"));
    private static final EnumDirection[] a = EnumDirection.values();
    public final List<TileEntity> tileEntityList = Lists.newArrayList();
    public final List<TileEntity> tileEntityListTick = Lists.newArrayList();
    protected final List<TileEntity> tileEntityListPending = Lists.newArrayList();
    protected final List<TileEntity> tileEntityListUnload = Lists.newArrayList();
    public final Thread serverThread;
    private final boolean debugWorld;
    private int d;
    protected int n = (new Random()).nextInt();
    protected final int o = 1013904223;
    protected float lastRainLevel;
    public float rainLevel;
    protected float lastThunderLevel;
    public float thunderLevel;
    public final Random random = new Random();
    private final DimensionManager x;
    public final WorldDataMutable worldData;
    private final Supplier<GameProfilerFiller> methodProfiler;
    public final boolean isClientSide;
    protected boolean tickingTileEntities;
    private final WorldBorder worldBorder;
    private final BiomeManager biomeManager;
    private final ResourceKey<World> dimensionKey;

    // CraftBukkit start Added the following
    private final ResourceKey<DimensionManager> typeKey;
    private final CraftWorld world;
    public boolean pvpMode;
    public boolean keepSpawnInMemory = true;
    public org.bukkit.generator.ChunkGenerator generator;

    public boolean captureBlockStates = false;
    public boolean captureTreeGeneration = false;
    public Map<BlockPosition, CapturedBlockState> capturedBlockStates = new java.util.LinkedHashMap<>();
    public Map<BlockPosition, TileEntity> capturedTileEntities = new HashMap<>();
    public List<EntityItem> captureDrops;
    public long ticksPerAnimalSpawns;
    public long ticksPerMonsterSpawns;
    public long ticksPerWaterSpawns;
    public long ticksPerWaterAmbientSpawns;
    public long ticksPerAmbientSpawns;
    public boolean populating;
    public final org.spigotmc.SpigotWorldConfig spigotConfig; // Spigot

    public final SpigotTimings.WorldTimingsHandler timings; // Spigot
    public static BlockPosition lastPhysicsProblem; // Spigot
    private org.spigotmc.TickLimiter entityLimiter;
    private org.spigotmc.TickLimiter tileLimiter;
    private int tileTickPosition;

    public CraftWorld getWorld() {
        return this.world;
    }

    public CraftServer getServer() {
        return (CraftServer) Bukkit.getServer();
    }

    public ResourceKey<DimensionManager> getTypeKey() {
        return typeKey;
    }

    protected World(WorldDataMutable worlddatamutable, ResourceKey<World> resourcekey, final DimensionManager dimensionmanager, Supplier<GameProfilerFiller> supplier, boolean flag, boolean flag1, long i, org.bukkit.generator.ChunkGenerator gen, org.bukkit.World.Environment env) {
        this.spigotConfig = new org.spigotmc.SpigotWorldConfig(((net.minecraft.world.level.storage.WorldDataServer) worlddatamutable).getName()); // Spigot
        this.generator = gen;
        this.world = new CraftWorld((WorldServer) this, gen, env);
        this.ticksPerAnimalSpawns = this.getServer().getTicksPerAnimalSpawns(); // CraftBukkit
        this.ticksPerMonsterSpawns = this.getServer().getTicksPerMonsterSpawns(); // CraftBukkit
        this.ticksPerWaterSpawns = this.getServer().getTicksPerWaterSpawns(); // CraftBukkit
        this.ticksPerWaterAmbientSpawns = this.getServer().getTicksPerWaterAmbientSpawns(); // CraftBukkit
        this.ticksPerAmbientSpawns = this.getServer().getTicksPerAmbientSpawns(); // CraftBukkit
        this.typeKey = (ResourceKey) this.getServer().getHandle().getServer().customRegistry.a().c(dimensionmanager).orElseThrow(() -> {
            return new IllegalStateException("Unregistered dimension type: " + dimensionmanager);
        });
        // CraftBukkit end
        this.methodProfiler = supplier;
        this.worldData = worlddatamutable;
        this.x = dimensionmanager;
        this.dimensionKey = resourcekey;
        this.isClientSide = flag;
        if (dimensionmanager.getCoordinateScale() != 1.0D) {
            this.worldBorder = new WorldBorder() {
                @Override
                public double getCenterX() {
                    return super.getCenterX(); // CraftBukkit
                }

                @Override
                public double getCenterZ() {
                    return super.getCenterZ(); // CraftBukkit
                }
            };
        } else {
            this.worldBorder = new WorldBorder();
        }

        this.serverThread = Thread.currentThread();
        this.biomeManager = new BiomeManager(this, i, dimensionmanager.getGenLayerZoomer());
        this.debugWorld = flag1;
        // CraftBukkit start
        getWorldBorder().world = (WorldServer) this;
        // From PlayerList.setPlayerFileData
        getWorldBorder().a(new IWorldBorderListener() {
            public void a(WorldBorder worldborder, double d0) {
                getServer().getHandle().sendAll(new PacketPlayOutWorldBorder(worldborder, PacketPlayOutWorldBorder.EnumWorldBorderAction.SET_SIZE), worldborder.world);
            }

            public void a(WorldBorder worldborder, double d0, double d1, long i) {
                getServer().getHandle().sendAll(new PacketPlayOutWorldBorder(worldborder, PacketPlayOutWorldBorder.EnumWorldBorderAction.LERP_SIZE), worldborder.world);
            }

            public void a(WorldBorder worldborder, double d0, double d1) {
                getServer().getHandle().sendAll(new PacketPlayOutWorldBorder(worldborder, PacketPlayOutWorldBorder.EnumWorldBorderAction.SET_CENTER), worldborder.world);
            }

            public void a(WorldBorder worldborder, int i) {
                getServer().getHandle().sendAll(new PacketPlayOutWorldBorder(worldborder, PacketPlayOutWorldBorder.EnumWorldBorderAction.SET_WARNING_TIME), worldborder.world);
            }

            public void b(WorldBorder worldborder, int i) {
                getServer().getHandle().sendAll(new PacketPlayOutWorldBorder(worldborder, PacketPlayOutWorldBorder.EnumWorldBorderAction.SET_WARNING_BLOCKS), worldborder.world);
            }

            public void b(WorldBorder worldborder, double d0) {}

            public void c(WorldBorder worldborder, double d0) {}
        });
        // CraftBukkit end
        timings = new SpigotTimings.WorldTimingsHandler(this); // Spigot - code below can generate new world and access timings
        this.entityLimiter = new org.spigotmc.TickLimiter(spigotConfig.entityMaxTickTime);
        this.tileLimiter = new org.spigotmc.TickLimiter(spigotConfig.tileMaxTickTime);
    }

    @Override
    public boolean s_() {
        return this.isClientSide;
    }

    @Nullable
    public MinecraftServer getMinecraftServer() {
        return null;
    }

    public static boolean isValidLocation(BlockPosition blockposition) {
        return !isOutsideWorld(blockposition) && D(blockposition);
    }

    public static boolean l(BlockPosition blockposition) {
        return !d(blockposition.getY()) && D(blockposition);
    }

    private static boolean D(BlockPosition blockposition) {
        return blockposition.getX() >= -30000000 && blockposition.getZ() >= -30000000 && blockposition.getX() < 30000000 && blockposition.getZ() < 30000000;
    }

    private static boolean d(int i) {
        return i < -20000000 || i >= 20000000;
    }

    public static boolean isOutsideWorld(BlockPosition blockposition) {
        return b(blockposition.getY());
    }

    public static boolean b(int i) {
        return i < 0 || i >= 256;
    }

    public Chunk getChunkAtWorldCoords(BlockPosition blockposition) {
        return this.getChunkAt(blockposition.getX() >> 4, blockposition.getZ() >> 4);
    }

    @Override
    public Chunk getChunkAt(int i, int j) {
        return (Chunk) this.getChunkAt(i, j, ChunkStatus.FULL);
    }

    @Override
    public IChunkAccess getChunkAt(int i, int j, ChunkStatus chunkstatus, boolean flag) {
        IChunkAccess ichunkaccess = this.getChunkProvider().getChunkAt(i, j, chunkstatus, flag);

        if (ichunkaccess == null && flag) {
            throw new IllegalStateException("Should always be able to create a chunk!");
        } else {
            return ichunkaccess;
        }
    }

    @Override
    public boolean setTypeAndData(BlockPosition blockposition, IBlockData iblockdata, int i) {
        return this.a(blockposition, iblockdata, i, 512);
    }

    @Override
    public boolean a(BlockPosition blockposition, IBlockData iblockdata, int i, int j) {
        // CraftBukkit start - tree generation
        if (this.captureTreeGeneration) {
            CapturedBlockState blockstate = capturedBlockStates.get(blockposition);
            if (blockstate == null) {
                blockstate = CapturedBlockState.getTreeBlockState(this, blockposition, i);
                this.capturedBlockStates.put(blockposition.immutableCopy(), blockstate);
            }
            blockstate.setData(iblockdata);
            return true;
        }
        // CraftBukkit end
        if (isOutsideWorld(blockposition)) {
            return false;
        } else if (!this.isClientSide && this.isDebugWorld()) {
            return false;
        } else {
            Chunk chunk = this.getChunkAtWorldCoords(blockposition);
            Block block = iblockdata.getBlock();

            // CraftBukkit start - capture blockstates
            boolean captured = false;
            if (this.captureBlockStates && !this.capturedBlockStates.containsKey(blockposition)) {
                CapturedBlockState blockstate = CapturedBlockState.getBlockState(this, blockposition, i);
                this.capturedBlockStates.put(blockposition.immutableCopy(), blockstate);
                captured = true;
            }
            // CraftBukkit end

            IBlockData iblockdata1 = chunk.setType(blockposition, iblockdata, (i & 64) != 0, (i & 1024) == 0); // CraftBukkit custom NO_PLACE flag

            if (iblockdata1 == null) {
                // CraftBukkit start - remove blockstate if failed (or the same)
                if (this.captureBlockStates && captured) {
                    this.capturedBlockStates.remove(blockposition);
                }
                // CraftBukkit end
                return false;
            } else {
                IBlockData iblockdata2 = this.getType(blockposition);

                if ((i & 128) == 0 && iblockdata2 != iblockdata1 && (iblockdata2.b((IBlockAccess) this, blockposition) != iblockdata1.b((IBlockAccess) this, blockposition) || iblockdata2.f() != iblockdata1.f() || iblockdata2.e() || iblockdata1.e())) {
                    this.getMethodProfiler().enter("queueCheckLight");
                    this.getChunkProvider().getLightEngine().a(blockposition);
                    this.getMethodProfiler().exit();
                }

                /*
                if (iblockdata2 == iblockdata) {
                    if (iblockdata1 != iblockdata2) {
                        this.b(blockposition, iblockdata1, iblockdata2);
                    }

                    if ((i & 2) != 0 && (!this.isClientSide || (i & 4) == 0) && (this.isClientSide || chunk.getState() != null && chunk.getState().isAtLeast(PlayerChunk.State.TICKING))) {
                        this.notify(blockposition, iblockdata1, iblockdata, i);
                    }

                    if ((i & 1) != 0) {
                        this.update(blockposition, iblockdata1.getBlock());
                        if (!this.isClientSide && iblockdata.isComplexRedstone()) {
                            this.updateAdjacentComparators(blockposition, block);
                        }
                    }

                    if ((i & 16) == 0 && j > 0) {
                        int k = i & -34;

                        iblockdata1.b(this, blockposition, k, j - 1);
                        iblockdata.a((GeneratorAccess) this, blockposition, k, j - 1);
                        iblockdata.b(this, blockposition, k, j - 1);
                    }

                    this.a(blockposition, iblockdata1, iblockdata2);
                }
                */

                // CraftBukkit start
                if (!this.captureBlockStates) { // Don't notify clients or update physics while capturing blockstates
                    // Modularize client and physic updates
                    // Spigot start
                    try {
                        notifyAndUpdatePhysics(blockposition, chunk, iblockdata1, iblockdata, iblockdata2, i, j);
                    } catch (StackOverflowError ex) {
                        lastPhysicsProblem = new BlockPosition(blockposition);
                    }
                    // Spigot end
                }
                // CraftBukkit end

                return true;
            }
        }
    }

    // CraftBukkit start - Split off from above in order to directly send client and physic updates
    public void notifyAndUpdatePhysics(BlockPosition blockposition, Chunk chunk, IBlockData oldBlock, IBlockData newBlock, IBlockData actualBlock, int i, int j) {
        IBlockData iblockdata = newBlock;
        IBlockData iblockdata1 = oldBlock;
        IBlockData iblockdata2 = actualBlock;
        if (iblockdata2 == iblockdata) {
            if (iblockdata1 != iblockdata2) {
                this.b(blockposition, iblockdata1, iblockdata2);
            }

            if ((i & 2) != 0 && (!this.isClientSide || (i & 4) == 0) && (this.isClientSide || chunk == null || (chunk.getState() != null && chunk.getState().isAtLeast(PlayerChunk.State.TICKING)))) { // allow chunk to be null here as chunk.isReady() is false when we send our notification during block placement
                this.notify(blockposition, iblockdata1, iblockdata, i);
            }

            if ((i & 1) != 0) {
                this.update(blockposition, iblockdata1.getBlock());
                if (!this.isClientSide && iblockdata.isComplexRedstone()) {
                    this.updateAdjacentComparators(blockposition, newBlock.getBlock());
                }
            }

            if ((i & 16) == 0 && j > 0) {
                int k = i & -34;

                // CraftBukkit start
                iblockdata1.b(this, blockposition, k, j - 1); // Don't call an event for the old block to limit event spam
                CraftWorld world = ((WorldServer) this).getWorld();
                if (world != null) {
                    BlockPhysicsEvent event = new BlockPhysicsEvent(world.getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ()), CraftBlockData.fromData(iblockdata));
                    this.getServer().getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        return;
                    }
                }
                // CraftBukkit end
                iblockdata.a((GeneratorAccess) this, blockposition, k, j - 1);
                iblockdata.b(this, blockposition, k, j - 1);
            }

            this.a(blockposition, iblockdata1, iblockdata2);
        }
    }
    // CraftBukkit end

    public void a(BlockPosition blockposition, IBlockData iblockdata, IBlockData iblockdata1) {}

    @Override
    public boolean a(BlockPosition blockposition, boolean flag) {
        Fluid fluid = this.getFluid(blockposition);

        return this.setTypeAndData(blockposition, fluid.getBlockData(), 3 | (flag ? 64 : 0));
    }

    @Override
    public boolean a(BlockPosition blockposition, boolean flag, @Nullable Entity entity, int i) {
        IBlockData iblockdata = this.getType(blockposition);

        if (iblockdata.isAir()) {
            return false;
        } else {
            Fluid fluid = this.getFluid(blockposition);

            if (!(iblockdata.getBlock() instanceof BlockFireAbstract)) {
                this.triggerEffect(2001, blockposition, Block.getCombinedId(iblockdata));
            }

            if (flag) {
                TileEntity tileentity = iblockdata.getBlock().isTileEntity() ? this.getTileEntity(blockposition) : null;

                Block.dropItems(iblockdata, this, blockposition, tileentity, entity, ItemStack.b);
            }

            return this.a(blockposition, fluid.getBlockData(), 3, i);
        }
    }

    public boolean setTypeUpdate(BlockPosition blockposition, IBlockData iblockdata) {
        return this.setTypeAndData(blockposition, iblockdata, 3);
    }

    public abstract void notify(BlockPosition blockposition, IBlockData iblockdata, IBlockData iblockdata1, int i);

    public void b(BlockPosition blockposition, IBlockData iblockdata, IBlockData iblockdata1) {}

    public void applyPhysics(BlockPosition blockposition, Block block) {
        this.a(blockposition.west(), block, blockposition);
        this.a(blockposition.east(), block, blockposition);
        this.a(blockposition.down(), block, blockposition);
        this.a(blockposition.up(), block, blockposition);
        this.a(blockposition.north(), block, blockposition);
        this.a(blockposition.south(), block, blockposition);
    }

    public void a(BlockPosition blockposition, Block block, EnumDirection enumdirection) {
        if (enumdirection != EnumDirection.WEST) {
            this.a(blockposition.west(), block, blockposition);
        }

        if (enumdirection != EnumDirection.EAST) {
            this.a(blockposition.east(), block, blockposition);
        }

        if (enumdirection != EnumDirection.DOWN) {
            this.a(blockposition.down(), block, blockposition);
        }

        if (enumdirection != EnumDirection.UP) {
            this.a(blockposition.up(), block, blockposition);
        }

        if (enumdirection != EnumDirection.NORTH) {
            this.a(blockposition.north(), block, blockposition);
        }

        if (enumdirection != EnumDirection.SOUTH) {
            this.a(blockposition.south(), block, blockposition);
        }

    }

    public void a(BlockPosition blockposition, Block block, BlockPosition blockposition1) {
        if (!this.isClientSide) {
            IBlockData iblockdata = this.getType(blockposition);

            try {
                // CraftBukkit start
                CraftWorld world = ((WorldServer) this).getWorld();
                if (world != null) {
                    BlockPhysicsEvent event = new BlockPhysicsEvent(world.getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ()), CraftBlockData.fromData(iblockdata), world.getBlockAt(blockposition1.getX(), blockposition1.getY(), blockposition1.getZ()));
                    this.getServer().getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        return;
                    }
                }
                // CraftBukkit end
                iblockdata.doPhysics(this, blockposition, block, blockposition1, false);
            // Spigot Start
            } catch (StackOverflowError ex) {
                lastPhysicsProblem = new BlockPosition(blockposition);
                // Spigot End
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.a(throwable, "Exception while updating neighbours");
                CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Block being updated");

                crashreportsystemdetails.a("Source block type", () -> {
                    try {
                        return String.format("ID #%s (%s // %s)", IRegistry.BLOCK.getKey(block), block.i(), block.getClass().getCanonicalName());
                    } catch (Throwable throwable1) {
                        return "ID #" + IRegistry.BLOCK.getKey(block);
                    }
                });
                CrashReportSystemDetails.a(crashreportsystemdetails, blockposition, iblockdata);
                throw new ReportedException(crashreport);
            }
        }
    }

    @Override
    public int a(HeightMap.Type heightmap_type, int i, int j) {
        int k;

        if (i >= -30000000 && j >= -30000000 && i < 30000000 && j < 30000000) {
            if (this.isChunkLoaded(i >> 4, j >> 4)) {
                k = this.getChunkAt(i >> 4, j >> 4).getHighestBlock(heightmap_type, i & 15, j & 15) + 1;
            } else {
                k = 0;
            }
        } else {
            k = this.getSeaLevel() + 1;
        }

        return k;
    }

    @Override
    public LightEngine e() {
        return this.getChunkProvider().getLightEngine();
    }

    @Override
    public IBlockData getType(BlockPosition blockposition) {
        // CraftBukkit start - tree generation
        if (captureTreeGeneration) {
            CapturedBlockState previous = capturedBlockStates.get(blockposition);
            if (previous != null) {
                return previous.getHandle();
            }
        }
        // CraftBukkit end
        if (isOutsideWorld(blockposition)) {
            return Blocks.VOID_AIR.getBlockData();
        } else {
            Chunk chunk = this.getChunkAt(blockposition.getX() >> 4, blockposition.getZ() >> 4);

            return chunk.getType(blockposition);
        }
    }

    @Override
    public Fluid getFluid(BlockPosition blockposition) {
        if (isOutsideWorld(blockposition)) {
            return FluidTypes.EMPTY.h();
        } else {
            Chunk chunk = this.getChunkAtWorldCoords(blockposition);

            return chunk.getFluid(blockposition);
        }
    }

    public boolean isDay() {
        return !this.getDimensionManager().isFixedTime() && this.d < 4;
    }

    public boolean isNight() {
        return !this.getDimensionManager().isFixedTime() && !this.isDay();
    }

    @Override
    public void playSound(@Nullable EntityHuman entityhuman, BlockPosition blockposition, SoundEffect soundeffect, SoundCategory soundcategory, float f, float f1) {
        this.playSound(entityhuman, (double) blockposition.getX() + 0.5D, (double) blockposition.getY() + 0.5D, (double) blockposition.getZ() + 0.5D, soundeffect, soundcategory, f, f1);
    }

    public abstract void playSound(@Nullable EntityHuman entityhuman, double d0, double d1, double d2, SoundEffect soundeffect, SoundCategory soundcategory, float f, float f1);

    public abstract void playSound(@Nullable EntityHuman entityhuman, Entity entity, SoundEffect soundeffect, SoundCategory soundcategory, float f, float f1);

    public void a(double d0, double d1, double d2, SoundEffect soundeffect, SoundCategory soundcategory, float f, float f1, boolean flag) {}

    @Override
    public void addParticle(ParticleParam particleparam, double d0, double d1, double d2, double d3, double d4, double d5) {}

    public void b(ParticleParam particleparam, double d0, double d1, double d2, double d3, double d4, double d5) {}

    public void b(ParticleParam particleparam, boolean flag, double d0, double d1, double d2, double d3, double d4, double d5) {}

    public float a(float f) {
        float f1 = this.f(f);

        return f1 * 6.2831855F;
    }

    public boolean a(TileEntity tileentity) {
        if (this.tickingTileEntities) {
            World.LOGGER.error("Adding block entity while ticking: {} @ {}", new org.apache.logging.log4j.util.Supplier[]{() -> {
                        return IRegistry.BLOCK_ENTITY_TYPE.getKey(tileentity.getTileType());
                    }, tileentity::getPosition});
        }

        boolean flag = this.tileEntityList.add(tileentity);

        if (flag && tileentity instanceof ITickable) {
            this.tileEntityListTick.add(tileentity);
        }

        if (this.isClientSide) {
            BlockPosition blockposition = tileentity.getPosition();
            IBlockData iblockdata = this.getType(blockposition);

            this.notify(blockposition, iblockdata, iblockdata, 2);
        }

        return flag;
    }

    public void a(Collection<TileEntity> collection) {
        if (this.tickingTileEntities) {
            this.tileEntityListPending.addAll(collection);
        } else {
            Iterator iterator = collection.iterator();

            while (iterator.hasNext()) {
                TileEntity tileentity = (TileEntity) iterator.next();

                this.a(tileentity);
            }
        }

    }

    public void tickBlockEntities() {
        GameProfilerFiller gameprofilerfiller = this.getMethodProfiler();

        gameprofilerfiller.enter("blockEntities");
        timings.tileEntityTick.startTiming(); // Spigot
        if (!this.tileEntityListUnload.isEmpty()) {
            this.tileEntityListTick.removeAll(this.tileEntityListUnload);
            this.tileEntityList.removeAll(this.tileEntityListUnload);
            this.tileEntityListUnload.clear();
        }

        this.tickingTileEntities = true;
        // Spigot start
        // Iterator iterator = this.tileEntityListTick.iterator();
        int tilesThisCycle = 0;
        for (tileLimiter.initTick();
                tilesThisCycle < tileEntityListTick.size() && (tilesThisCycle % 10 != 0 || tileLimiter.shouldContinue());
                tileTickPosition++, tilesThisCycle++) {
            tileTickPosition = (tileTickPosition < tileEntityListTick.size()) ? tileTickPosition : 0;
            TileEntity tileentity = (TileEntity) this.tileEntityListTick.get(tileTickPosition);
            // Spigot start
            if (tileentity == null) {
                getServer().getLogger().severe("Spigot has detected a null entity and has removed it, preventing a crash");
                tilesThisCycle--;
                this.tileEntityListTick.remove(tileTickPosition--);
                continue;
            }
            // Spigot end

            if (!tileentity.isRemoved() && tileentity.hasWorld()) {
                BlockPosition blockposition = tileentity.getPosition();

                if (this.getChunkProvider().a(blockposition) && this.getWorldBorder().a(blockposition)) {
                    try {
                        gameprofilerfiller.a(() -> {
                            return String.valueOf(TileEntityTypes.a(tileentity.getTileType()));
                        });
                        tileentity.tickTimer.startTiming(); // Spigot
                        if (tileentity.getTileType().isValidBlock(this.getType(blockposition).getBlock())) {
                            ((ITickable) tileentity).tick();
                        } else {
                            tileentity.w();
                        }

                        gameprofilerfiller.exit();
                    } catch (Throwable throwable) {
                        CrashReport crashreport = CrashReport.a(throwable, "Ticking block entity");
                        CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Block entity being ticked");

                        tileentity.a(crashreportsystemdetails);
                        throw new ReportedException(crashreport);
                        // Spigot start
                    } finally {
                        tileentity.tickTimer.stopTiming();
                        // Spigot end
                    }
                }
            }

            if (tileentity.isRemoved()) {
                // Spigot start
                tilesThisCycle--;
                this.tileEntityListTick.remove(tileTickPosition--);
                // Spigot end
                this.tileEntityList.remove(tileentity);
                if (this.isLoaded(tileentity.getPosition())) {
                    this.getChunkAtWorldCoords(tileentity.getPosition()).removeTileEntity(tileentity.getPosition());
                }
            }
        }

        timings.tileEntityTick.stopTiming(); // Spigot
        timings.tileEntityPending.startTiming(); // Spigot
        this.tickingTileEntities = false;
        gameprofilerfiller.exitEnter("pendingBlockEntities");
        if (!this.tileEntityListPending.isEmpty()) {
            for (int i = 0; i < this.tileEntityListPending.size(); ++i) {
                TileEntity tileentity1 = (TileEntity) this.tileEntityListPending.get(i);

                if (!tileentity1.isRemoved()) {
                    /* CraftBukkit start - Order matters, moved down
                    if (!this.tileEntityList.contains(tileentity1)) {
                        this.a(tileentity1);
                    }
                    // CraftBukkit end */

                    if (this.isLoaded(tileentity1.getPosition())) {
                        Chunk chunk = this.getChunkAtWorldCoords(tileentity1.getPosition());
                        IBlockData iblockdata = chunk.getType(tileentity1.getPosition());

                        chunk.setTileEntity(tileentity1.getPosition(), tileentity1);
                        this.notify(tileentity1.getPosition(), iblockdata, iblockdata, 3);
                        // CraftBukkit start
                        // From above, don't screw this up - SPIGOT-1746
                        if (!this.tileEntityList.contains(tileentity1)) {
                            this.a(tileentity1);
                        }
                        // CraftBukkit end
                    }
                }
            }

            this.tileEntityListPending.clear();
        }

        timings.tileEntityPending.stopTiming(); // Spigot
        gameprofilerfiller.exit();
        spigotConfig.currentPrimedTnt = 0; // Spigot
    }

    public void a(Consumer<Entity> consumer, Entity entity) {
        try {
            SpigotTimings.tickEntityTimer.startTiming(); // Spigot
            consumer.accept(entity);
            SpigotTimings.tickEntityTimer.stopTiming(); // Spigot
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.a(throwable, "Ticking entity");
            CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Entity being ticked");

            entity.appendEntityCrashDetails(crashreportsystemdetails);
            throw new ReportedException(crashreport);
        }
    }

    public Explosion explode(@Nullable Entity entity, double d0, double d1, double d2, float f, Explosion.Effect explosion_effect) {
        return this.createExplosion(entity, (DamageSource) null, (ExplosionDamageCalculator) null, d0, d1, d2, f, false, explosion_effect);
    }

    public Explosion createExplosion(@Nullable Entity entity, double d0, double d1, double d2, float f, boolean flag, Explosion.Effect explosion_effect) {
        return this.createExplosion(entity, (DamageSource) null, (ExplosionDamageCalculator) null, d0, d1, d2, f, flag, explosion_effect);
    }

    public Explosion createExplosion(@Nullable Entity entity, @Nullable DamageSource damagesource, @Nullable ExplosionDamageCalculator explosiondamagecalculator, double d0, double d1, double d2, float f, boolean flag, Explosion.Effect explosion_effect) {
        Explosion explosion = new Explosion(this, entity, damagesource, explosiondamagecalculator, d0, d1, d2, f, flag, explosion_effect);

        explosion.a();
        explosion.a(true);
        return explosion;
    }

    public String P() {
        return this.getChunkProvider().getName();
    }

    @Nullable
    @Override
    // CraftBukkit start
    public TileEntity getTileEntity(BlockPosition blockposition) {
        return getTileEntity(blockposition, true);
    }

    @Nullable
    public TileEntity getTileEntity(BlockPosition blockposition, boolean validate) {
        // CraftBukkit end
        if (isOutsideWorld(blockposition)) {
            return null;
        } else if (!this.isClientSide && Thread.currentThread() != this.serverThread) {
            return null;
        } else {
            // CraftBukkit start
            if (capturedTileEntities.containsKey(blockposition)) {
                return capturedTileEntities.get(blockposition);
            }
            // CraftBukkit end

            TileEntity tileentity = null;

            if (this.tickingTileEntities) {
                tileentity = this.E(blockposition);
            }

            if (tileentity == null) {
                tileentity = this.getChunkAtWorldCoords(blockposition).a(blockposition, Chunk.EnumTileEntityState.IMMEDIATE);
            }

            if (tileentity == null) {
                tileentity = this.E(blockposition);
            }

            return tileentity;
        }
    }

    @Nullable
    private TileEntity E(BlockPosition blockposition) {
        for (int i = 0; i < this.tileEntityListPending.size(); ++i) {
            TileEntity tileentity = (TileEntity) this.tileEntityListPending.get(i);

            if (!tileentity.isRemoved() && tileentity.getPosition().equals(blockposition)) {
                return tileentity;
            }
        }

        return null;
    }

    public void setTileEntity(BlockPosition blockposition, @Nullable TileEntity tileentity) {
        if (!isOutsideWorld(blockposition)) {
            if (tileentity != null && !tileentity.isRemoved()) {
                // CraftBukkit start
                if (captureBlockStates) {
                    tileentity.setLocation(this, blockposition);
                    capturedTileEntities.put(blockposition.immutableCopy(), tileentity);
                    return;
                }
                // CraftBukkit end
                if (this.tickingTileEntities) {
                    tileentity.setLocation(this, blockposition);
                    Iterator iterator = this.tileEntityListPending.iterator();

                    while (iterator.hasNext()) {
                        TileEntity tileentity1 = (TileEntity) iterator.next();

                        if (tileentity1.getPosition().equals(blockposition)) {
                            tileentity1.al_();
                            iterator.remove();
                        }
                    }

                    this.tileEntityListPending.add(tileentity);
                } else {
                    this.getChunkAtWorldCoords(blockposition).setTileEntity(blockposition, tileentity);
                    this.a(tileentity);
                }
            }

        }
    }

    public void removeTileEntity(BlockPosition blockposition) {
        TileEntity tileentity = this.getTileEntity(blockposition, false); // CraftBukkit

        if (tileentity != null && this.tickingTileEntities) {
            tileentity.al_();
            this.tileEntityListPending.remove(tileentity);
        } else {
            if (tileentity != null) {
                this.tileEntityListPending.remove(tileentity);
                this.tileEntityList.remove(tileentity);
                this.tileEntityListTick.remove(tileentity);
            }

            this.getChunkAtWorldCoords(blockposition).removeTileEntity(blockposition);
        }

    }

    public boolean p(BlockPosition blockposition) {
        return isOutsideWorld(blockposition) ? false : this.getChunkProvider().b(blockposition.getX() >> 4, blockposition.getZ() >> 4);
    }

    public boolean a(BlockPosition blockposition, Entity entity, EnumDirection enumdirection) {
        if (isOutsideWorld(blockposition)) {
            return false;
        } else {
            IChunkAccess ichunkaccess = this.getChunkAt(blockposition.getX() >> 4, blockposition.getZ() >> 4, ChunkStatus.FULL, false);

            return ichunkaccess == null ? false : ichunkaccess.getType(blockposition).a((IBlockAccess) this, blockposition, entity, enumdirection);
        }
    }

    public boolean a(BlockPosition blockposition, Entity entity) {
        return this.a(blockposition, entity, EnumDirection.UP);
    }

    public void Q() {
        double d0 = 1.0D - (double) (this.d(1.0F) * 5.0F) / 16.0D;
        double d1 = 1.0D - (double) (this.b(1.0F) * 5.0F) / 16.0D;
        double d2 = 0.5D + 2.0D * MathHelper.a((double) MathHelper.cos(this.f(1.0F) * 6.2831855F), -0.25D, 0.25D);

        this.d = (int) ((1.0D - d2 * d0 * d1) * 11.0D);
    }

    public void setSpawnFlags(boolean flag, boolean flag1) {
        this.getChunkProvider().a(flag, flag1);
    }

    protected void R() {
        if (this.worldData.hasStorm()) {
            this.rainLevel = 1.0F;
            if (this.worldData.isThundering()) {
                this.thunderLevel = 1.0F;
            }
        }

    }

    public void close() throws IOException {
        this.getChunkProvider().close();
    }

    @Nullable
    @Override
    public IBlockAccess c(int i, int j) {
        return this.getChunkAt(i, j, ChunkStatus.FULL, false);
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity entity, AxisAlignedBB axisalignedbb, @Nullable Predicate<? super Entity> predicate) {
        this.getMethodProfiler().c("getEntities");
        List<Entity> list = Lists.newArrayList();
        int i = MathHelper.floor((axisalignedbb.minX - 2.0D) / 16.0D);
        int j = MathHelper.floor((axisalignedbb.maxX + 2.0D) / 16.0D);
        int k = MathHelper.floor((axisalignedbb.minZ - 2.0D) / 16.0D);
        int l = MathHelper.floor((axisalignedbb.maxZ + 2.0D) / 16.0D);
        IChunkProvider ichunkprovider = this.getChunkProvider();

        for (int i1 = i; i1 <= j; ++i1) {
            for (int j1 = k; j1 <= l; ++j1) {
                Chunk chunk = ichunkprovider.getChunkAt(i1, j1, false);

                if (chunk != null) {
                    chunk.a(entity, axisalignedbb, list, predicate);
                }
            }
        }

        return list;
    }

    public <T extends Entity> List<T> a(@Nullable EntityTypes<T> entitytypes, AxisAlignedBB axisalignedbb, Predicate<? super T> predicate) {
        this.getMethodProfiler().c("getEntities");
        int i = MathHelper.floor((axisalignedbb.minX - 2.0D) / 16.0D);
        int j = MathHelper.f((axisalignedbb.maxX + 2.0D) / 16.0D);
        int k = MathHelper.floor((axisalignedbb.minZ - 2.0D) / 16.0D);
        int l = MathHelper.f((axisalignedbb.maxZ + 2.0D) / 16.0D);
        List<T> list = Lists.newArrayList();

        for (int i1 = i; i1 < j; ++i1) {
            for (int j1 = k; j1 < l; ++j1) {
                Chunk chunk = this.getChunkProvider().getChunkAt(i1, j1, false);

                if (chunk != null) {
                    chunk.a(entitytypes, axisalignedbb, list, predicate);
                }
            }
        }

        return list;
    }

    @Override
    public <T extends Entity> List<T> a(Class<? extends T> oclass, AxisAlignedBB axisalignedbb, @Nullable Predicate<? super T> predicate) {
        this.getMethodProfiler().c("getEntities");
        int i = MathHelper.floor((axisalignedbb.minX - 2.0D) / 16.0D);
        int j = MathHelper.f((axisalignedbb.maxX + 2.0D) / 16.0D);
        int k = MathHelper.floor((axisalignedbb.minZ - 2.0D) / 16.0D);
        int l = MathHelper.f((axisalignedbb.maxZ + 2.0D) / 16.0D);
        List<T> list = Lists.newArrayList();
        IChunkProvider ichunkprovider = this.getChunkProvider();

        for (int i1 = i; i1 < j; ++i1) {
            for (int j1 = k; j1 < l; ++j1) {
                Chunk chunk = ichunkprovider.getChunkAt(i1, j1, false);

                if (chunk != null) {
                    chunk.a(oclass, axisalignedbb, list, predicate);
                }
            }
        }

        return list;
    }

    @Override
    public <T extends Entity> List<T> b(Class<? extends T> oclass, AxisAlignedBB axisalignedbb, @Nullable Predicate<? super T> predicate) {
        this.getMethodProfiler().c("getLoadedEntities");
        int i = MathHelper.floor((axisalignedbb.minX - 2.0D) / 16.0D);
        int j = MathHelper.f((axisalignedbb.maxX + 2.0D) / 16.0D);
        int k = MathHelper.floor((axisalignedbb.minZ - 2.0D) / 16.0D);
        int l = MathHelper.f((axisalignedbb.maxZ + 2.0D) / 16.0D);
        List<T> list = Lists.newArrayList();
        IChunkProvider ichunkprovider = this.getChunkProvider();

        for (int i1 = i; i1 < j; ++i1) {
            for (int j1 = k; j1 < l; ++j1) {
                Chunk chunk = ichunkprovider.a(i1, j1);

                if (chunk != null) {
                    chunk.a(oclass, axisalignedbb, list, predicate);
                }
            }
        }

        return list;
    }

    @Nullable
    public abstract Entity getEntity(int i);

    public void b(BlockPosition blockposition, TileEntity tileentity) {
        if (this.isLoaded(blockposition)) {
            this.getChunkAtWorldCoords(blockposition).markDirty();
        }

    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    public int getBlockPower(BlockPosition blockposition) {
        byte b0 = 0;
        int i = Math.max(b0, this.c(blockposition.down(), EnumDirection.DOWN));

        if (i >= 15) {
            return i;
        } else {
            i = Math.max(i, this.c(blockposition.up(), EnumDirection.UP));
            if (i >= 15) {
                return i;
            } else {
                i = Math.max(i, this.c(blockposition.north(), EnumDirection.NORTH));
                if (i >= 15) {
                    return i;
                } else {
                    i = Math.max(i, this.c(blockposition.south(), EnumDirection.SOUTH));
                    if (i >= 15) {
                        return i;
                    } else {
                        i = Math.max(i, this.c(blockposition.west(), EnumDirection.WEST));
                        if (i >= 15) {
                            return i;
                        } else {
                            i = Math.max(i, this.c(blockposition.east(), EnumDirection.EAST));
                            return i >= 15 ? i : i;
                        }
                    }
                }
            }
        }
    }

    public boolean isBlockFacePowered(BlockPosition blockposition, EnumDirection enumdirection) {
        return this.getBlockFacePower(blockposition, enumdirection) > 0;
    }

    public int getBlockFacePower(BlockPosition blockposition, EnumDirection enumdirection) {
        IBlockData iblockdata = this.getType(blockposition);
        int i = iblockdata.b((IBlockAccess) this, blockposition, enumdirection);

        return iblockdata.isOccluding(this, blockposition) ? Math.max(i, this.getBlockPower(blockposition)) : i;
    }

    public boolean isBlockIndirectlyPowered(BlockPosition blockposition) {
        return this.getBlockFacePower(blockposition.down(), EnumDirection.DOWN) > 0 ? true : (this.getBlockFacePower(blockposition.up(), EnumDirection.UP) > 0 ? true : (this.getBlockFacePower(blockposition.north(), EnumDirection.NORTH) > 0 ? true : (this.getBlockFacePower(blockposition.south(), EnumDirection.SOUTH) > 0 ? true : (this.getBlockFacePower(blockposition.west(), EnumDirection.WEST) > 0 ? true : this.getBlockFacePower(blockposition.east(), EnumDirection.EAST) > 0))));
    }

    public int s(BlockPosition blockposition) {
        int i = 0;
        EnumDirection[] aenumdirection = World.a;
        int j = aenumdirection.length;

        for (int k = 0; k < j; ++k) {
            EnumDirection enumdirection = aenumdirection[k];
            int l = this.getBlockFacePower(blockposition.shift(enumdirection), enumdirection);

            if (l >= 15) {
                return 15;
            }

            if (l > i) {
                i = l;
            }
        }

        return i;
    }

    public long getTime() {
        return this.worldData.getTime();
    }

    public long getDayTime() {
        return this.worldData.getDayTime();
    }

    public boolean a(EntityHuman entityhuman, BlockPosition blockposition) {
        return true;
    }

    public void broadcastEntityEffect(Entity entity, byte b0) {}

    public void playBlockAction(BlockPosition blockposition, Block block, int i, int j) {
        this.getType(blockposition).a(this, blockposition, i, j);
    }

    @Override
    public WorldData getWorldData() {
        return this.worldData;
    }

    public GameRules getGameRules() {
        return this.worldData.q();
    }

    public float b(float f) {
        return MathHelper.g(f, this.lastThunderLevel, this.thunderLevel) * this.d(f);
    }

    public float d(float f) {
        return MathHelper.g(f, this.lastRainLevel, this.rainLevel);
    }

    public boolean W() {
        return this.getDimensionManager().hasSkyLight() && !this.getDimensionManager().hasCeiling() ? (double) this.b(1.0F) > 0.9D : false;
    }

    public boolean isRaining() {
        return (double) this.d(1.0F) > 0.2D;
    }

    public boolean isRainingAt(BlockPosition blockposition) {
        if (!this.isRaining()) {
            return false;
        } else if (!this.e(blockposition)) {
            return false;
        } else if (this.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING, blockposition).getY() > blockposition.getY()) {
            return false;
        } else {
            BiomeBase biomebase = this.getBiome(blockposition);

            return biomebase.c() == BiomeBase.Precipitation.RAIN && biomebase.getAdjustedTemperature(blockposition) >= 0.15F;
        }
    }

    public boolean u(BlockPosition blockposition) {
        BiomeBase biomebase = this.getBiome(blockposition);

        return biomebase.d();
    }

    @Nullable
    public abstract WorldMap a(String s);

    public abstract void a(WorldMap worldmap);

    public abstract int getWorldMapCount();

    public void b(int i, BlockPosition blockposition, int j) {}

    public CrashReportSystemDetails a(CrashReport crashreport) {
        CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Affected level", 1);

        crashreportsystemdetails.a("All players", () -> {
            return this.getPlayers().size() + " total; " + this.getPlayers();
        });
        IChunkProvider ichunkprovider = this.getChunkProvider();

        crashreportsystemdetails.a("Chunk stats", ichunkprovider::getName);
        crashreportsystemdetails.a("Level dimension", () -> {
            return this.getDimensionKey().a().toString();
        });

        try {
            this.worldData.a(crashreportsystemdetails);
        } catch (Throwable throwable) {
            crashreportsystemdetails.a("Level Data Unobtainable", throwable);
        }

        return crashreportsystemdetails;
    }

    public abstract void a(int i, BlockPosition blockposition, int j);

    public abstract Scoreboard getScoreboard();

    public void updateAdjacentComparators(BlockPosition blockposition, Block block) {
        Iterator iterator = EnumDirection.EnumDirectionLimit.HORIZONTAL.iterator();

        while (iterator.hasNext()) {
            EnumDirection enumdirection = (EnumDirection) iterator.next();
            BlockPosition blockposition1 = blockposition.shift(enumdirection);

            if (this.isLoaded(blockposition1)) {
                IBlockData iblockdata = this.getType(blockposition1);

                if (iblockdata.a(Blocks.COMPARATOR)) {
                    iblockdata.doPhysics(this, blockposition1, block, blockposition, false);
                } else if (iblockdata.isOccluding(this, blockposition1)) {
                    blockposition1 = blockposition1.shift(enumdirection);
                    iblockdata = this.getType(blockposition1);
                    if (iblockdata.a(Blocks.COMPARATOR)) {
                        iblockdata.doPhysics(this, blockposition1, block, blockposition, false);
                    }
                }
            }
        }

    }

    @Override
    public DifficultyDamageScaler getDamageScaler(BlockPosition blockposition) {
        long i = 0L;
        float f = 0.0F;

        if (this.isLoaded(blockposition)) {
            f = this.af();
            i = this.getChunkAtWorldCoords(blockposition).getInhabitedTime();
        }

        return new DifficultyDamageScaler(this.getDifficulty(), this.getDayTime(), i, f);
    }

    @Override
    public int c() {
        return this.d;
    }

    public void c(int i) {}

    @Override
    public WorldBorder getWorldBorder() {
        return this.worldBorder;
    }

    public void a(Packet<?> packet) {
        throw new UnsupportedOperationException("Can't send packets to server unless you're on the client.");
    }

    @Override
    public DimensionManager getDimensionManager() {
        return this.x;
    }

    public ResourceKey<World> getDimensionKey() {
        return this.dimensionKey;
    }

    @Override
    public Random getRandom() {
        return this.random;
    }

    @Override
    public boolean a(BlockPosition blockposition, Predicate<IBlockData> predicate) {
        return predicate.test(this.getType(blockposition));
    }

    public abstract CraftingManager getCraftingManager();

    public abstract ITagRegistry p();

    public BlockPosition a(int i, int j, int k, int l) {
        this.n = this.n * 3 + 1013904223;
        int i1 = this.n >> 2;

        return new BlockPosition(i + (i1 & 15), j + (i1 >> 16 & l), k + (i1 >> 8 & 15));
    }

    public boolean isSavingDisabled() {
        return false;
    }

    public GameProfilerFiller getMethodProfiler() {
        return (GameProfilerFiller) this.methodProfiler.get();
    }

    public Supplier<GameProfilerFiller> getMethodProfilerSupplier() {
        return this.methodProfiler;
    }

    @Override
    public BiomeManager d() {
        return this.biomeManager;
    }

    public final boolean isDebugWorld() {
        return this.debugWorld;
    }
}
