package de.gurki.buddy.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.world.World;
import net.minecraft.state.property.Properties;

import static net.minecraft.block.Block.NOTIFY_LISTENERS;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Predicate;


public class Machines
{
    public static final String MOD_ID = "de.gurki.buddy";
	public static final Logger LOGGER = LogManager.getLogger( MOD_ID );

    public enum BuildMode {
        MarkOnly,
        ReadAndBuild,
        MarkAndBuild
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static void build( Geode geode, World world, BuildMode mode )
    {
        for ( Axis axis : Axis.values() )
        {
            clearArea( geode, axis, world );
            UnionFind uf = getComponents( geode, axis, world );

            if ( mode != BuildMode.ReadAndBuild ) {
                markComponentSupports( uf, geode, axis, world );
            }

            if ( mode == BuildMode.MarkOnly ) {
                continue;
            }

            buildAtMarks( uf, geode, axis, world );
        }
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static void markComponentSupports( UnionFind uf, Geode geode, Axis axis, World world )
    {
        clearMarkers( geode, axis, world );

        for ( HashSet<Integer> comp : uf.components )
        {
            Integer repId = comp.iterator().next();
            HashSet<BlockPos> cluster = new HashSet<>( comp.stream().map( i -> uf.graph.verts[ i ] ).toList() );
            Support support = Support.findClosestSupport( cluster, uf.graph, repId );

            if ( support.isEmpty() ) {
                LOGGER.info( "OH NO" );
                LOGGER.info( comp );
                LOGGER.info( support.seed );
                LOGGER.info( support.axis );
                continue;
            }

            support.getPositions().forEach( p -> world.setBlockState( p.offset( axis, 1 ), Blocks.RED_STAINED_GLASS.getDefaultState() ) );
            world.setBlockState( support.seed.offset( axis, 1 ), Blocks.ORANGE_STAINED_GLASS.getDefaultState() );
        }
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static ArrayList<Support> readMarkers( UnionFind uf, Geode geode, Axis axis, World world )
    {
        final int mar = Constants.kMargin;
        BlockPos boxMin = geode.min().add(-mar,-mar,-mar );
        BlockPos boxMax = geode.max().add( mar, mar, mar);

        final int off = Constants.kOffset + 1 - mar;
        BlockPos ll = boxMin.offset( axis, off + boxMax.getComponentAlongAxis( axis ) - boxMin.getComponentAlongAxis( axis ) );
        BlockPos tr = boxMax.offset( axis, off );

        HashSet<BlockPos> markers = new HashSet<>( BlockPos
            .stream( ll, tr )
            .filter( p -> {
                BlockState state = world.getBlockState( p );
                return state.isOf( Blocks.RED_STAINED_GLASS ) || state.isOf( Blocks.ORANGE_STAINED_GLASS );
            })
            .map( BlockPos::toImmutable )
            .toList()
        );

        ArrayList<Support> supports = new ArrayList<>();
        HashSet<Integer> doneCompIds = new HashSet<Integer>();

        for ( BlockPos p : markers )
        {
            BlockPos rep = p.offset( axis, -1 );
            Integer compId = uf.ids.get( uf.graph.ids.get( rep ) );

            if ( doneCompIds.contains( compId ) ) {
                continue;
            }

            HashSet<BlockPos> compMarkers = new HashSet<>( markers
                .stream()
                .filter( pos -> uf.ids.get( uf.graph.ids.get( pos.offset( axis, -1 ) ) ) == compId )
                .toList()
            );

            Support support = Support.findConnectedSupports( compMarkers, p );

            if ( ! support.isEmpty() ) {
                support.seed = support.seed.offset( axis,-1 );
                supports.add( support );
                doneCompIds.add( compId );
            }
        }

        return supports;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static void clearMarkers( Geode geode, Axis axis, World world )
    {
        final int mar = Constants.kMargin;
        BlockPos boxMin = geode.min().add(-mar,-mar,-mar );
        BlockPos boxMax = geode.max().add( mar, mar, mar);

        final int off = Constants.kOffset;
        BlockPos ll = boxMin.offset( axis, boxMax.getComponentAlongAxis( axis ) + off - mar + 1 - boxMin.getComponentAlongAxis( axis ) );
        BlockPos tr = boxMax.offset( axis, off - mar + 1 );

        BlockPos.iterate( ll, tr ).forEach( p -> world.setBlockState( p, Blocks.AIR.getDefaultState() ) );
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static void clearArea( Geode geode, Axis axis, World world )
    {
        final int dep = Constants.kDepth;
        final int mar = Constants.kMargin;
        BlockPos boxMin = geode.min().add(-mar,-mar,-mar );
        BlockPos boxMax = geode.max().add( mar, mar, mar);

        final int off = Constants.kOffset;
        BlockPos ll = boxMin.offset( axis, off - mar + 2 + boxMax.getComponentAlongAxis( axis ) - boxMin.getComponentAlongAxis( axis ) );
        BlockPos tr = boxMax.offset( axis, off - mar + dep - 2 );

        BlockPos.iterate( ll, tr ).forEach( p -> world.setBlockState( p, Blocks.AIR.getDefaultState() ) );
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static UnionFind getComponents( Geode geode, Axis axis, World world )
    {
        final int mar = Constants.kMargin;
        BlockPos boxMin = geode.min().add(-mar,-mar,-mar );
        BlockPos boxMax = geode.max().add( mar, mar, mar);

        final int off = Constants.kOffset;

        //  collect honey and slime blocks

        BlockPos ll = boxMin.offset( axis, boxMax.getComponentAlongAxis( axis ) + off - mar - boxMin.getComponentAlongAxis( axis ) );
        BlockPos tr = boxMax.offset( axis, off - mar );

        HashSet<BlockPos> positions = new HashSet<>();
        ArrayList<HashSet<BlockPos>> components = new ArrayList<>();
        components.add( new HashSet<>() );
        components.add( new HashSet<>() );

        for ( BlockPos p : BlockPos.iterate( ll, tr ) )
        {
            BlockState state = world.getBlockState( p );

            if ( state.isOf( Blocks.SLIME_BLOCK ) ) {
                positions.add( p.toImmutable() );
                components.get( 0 ).add( p.toImmutable() );
            } else if ( state.isOf( Blocks.HONEY_BLOCK ) ) {
                positions.add( p.toImmutable() );
                components.get( 1 ).add( p.toImmutable() );
            }
        }

        Graph graph = new Graph( positions );
        UnionFind uf = new UnionFind( graph );
        uf.setComponents( components );

        return uf;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static void buildAtMarks( UnionFind uf, Geode geode, Axis axis, World world )
    {
        //  build flying machine for each marked support

        ArrayList<Support> supports = readMarkers( uf, geode, axis, world );
        clearMarkers( geode, axis, world );

        for ( Support support : supports )
        {
            BlockPos rep = support.seed;
            boolean isSlime = world.getBlockState( rep ).isOf( Blocks.SLIME_BLOCK );
            Block block = isSlime ? Blocks.HONEY_BLOCK : Blocks.SLIME_BLOCK;

            buildAt(
                support.seed.offset( axis, 1 ),
                Direction.from( axis, AxisDirection.POSITIVE ),
                Direction.from( support.axis, AxisDirection.POSITIVE ),
                block,
                world
            );

            //  place blocker

            Integer repId = uf.graph.ids.get( rep );
            HashSet<Integer> comp = uf.components.get( uf.ids.get( repId ) );
            HashSet<Integer> supIds = support.getIndices( uf.graph );
            Integer blockerId = comp.stream().filter( Predicate.not( supIds::contains ) ).findAny().get();
            BlockPos blocker = uf.graph.verts[ blockerId ].offset( axis, 1 );
            world.setBlockState( blocker, Blocks.CRYING_OBSIDIAN.getDefaultState() );
        }
    }


    ////////////////////////////////////////////////////////////////////////////////
    //  with thanks adapted from kosma [1]
    //  [1] https://github.com/kosma/geodesy-fabric/blob/master/src/main/java/pl/kosma/geodesy/GeodesyCore.java#L363
    public static void buildAt( BlockPos pos, Direction directionAlong, Direction directionUp, Block stickyBlock, World world )
    {
        /*
         * It looks like this:
         * S HHH
         * S HVHH[<N
         * SB[L>]SSSB
         */

        // Blocker block.
        // world.setBlockState(blockerPos, Blocks.OBSIDIAN.getDefaultState(), NOTIFY_LISTENERS );

        // Clear out the machine marker blocks.
        world.setBlockState(pos.offset(directionUp, 0), Blocks.AIR.getDefaultState(), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 1), Blocks.AIR.getDefaultState(), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 2), Blocks.AIR.getDefaultState(), NOTIFY_LISTENERS );
        pos = pos.offset(directionAlong, 1);

        // First layer: piston, 2 slime
        world.setBlockState(pos.offset(directionUp, 0), Blocks.STICKY_PISTON.getDefaultState().with(Properties.FACING, directionAlong.getOpposite()), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 1), stickyBlock.getDefaultState(), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 2), stickyBlock.getDefaultState(), NOTIFY_LISTENERS );
        pos = pos.offset(directionAlong, 1);

        // Second layer: redstone lamp, observer, slime (order is important)
        world.setBlockState(pos.offset(directionUp, 2), stickyBlock.getDefaultState(), NOTIFY_LISTENERS );
        world.setBlockState( pos.offset(directionUp, 1), Blocks.OBSERVER.getDefaultState().with(Properties.FACING, directionUp), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 0), Blocks.REDSTONE_LAMP.getDefaultState(), NOTIFY_LISTENERS );
        pos = pos.offset(directionAlong, 1);

        // Third layer: observer, slime, slime
        world.setBlockState( pos.offset( directionUp, 0 ), Blocks.OBSERVER.getDefaultState().with(Properties.FACING, directionAlong.getOpposite()), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 1), stickyBlock.getDefaultState(), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 2), stickyBlock.getDefaultState(), NOTIFY_LISTENERS );
        pos = pos.offset(directionAlong, 1);

        // Fourth layer: piston, slime
        world.setBlockState(pos.offset(directionUp, 0), Blocks.STICKY_PISTON.getDefaultState().with(Properties.FACING, directionAlong), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 1), stickyBlock.getDefaultState(), NOTIFY_LISTENERS );
        pos = pos.offset(directionAlong, 1);

        // Fifth layer: slime, piston
        world.setBlockState(pos.offset(directionUp, 0), stickyBlock.getDefaultState(), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 1), Blocks.STICKY_PISTON.getDefaultState().with(Properties.FACING, directionAlong.getOpposite()), NOTIFY_LISTENERS );
        pos = pos.offset(directionAlong, 2);

        // [SKIP!] Seventh layer: slime, note block
        world.setBlockState(pos.offset(directionUp, 0), stickyBlock.getDefaultState(), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 1), Blocks.NOTE_BLOCK.getDefaultState(), NOTIFY_LISTENERS );
        pos = pos.offset(directionAlong, -1);

        // [GO BACK!] Sixth layer: slime, observer
        // This one is tricky, we initially set the observer in a wrong direction
        // so the note block tune change is not triggered.
        world.setBlockState( pos.offset(directionUp, 1), Blocks.OBSERVER.getDefaultState().with(Properties.FACING, directionUp), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 0), stickyBlock.getDefaultState(), NOTIFY_LISTENERS );
        world.setBlockState( pos.offset(directionUp, 1), Blocks.OBSERVER.getDefaultState().with(Properties.FACING, directionAlong), NOTIFY_LISTENERS );
        pos = pos.offset(directionAlong, 2);

        // [SKIP AGAIN!] Eighth layer: blocker
        world.setBlockState(pos.offset(directionUp, 0), Blocks.CRYING_OBSIDIAN.getDefaultState(), NOTIFY_LISTENERS );
    }
}
