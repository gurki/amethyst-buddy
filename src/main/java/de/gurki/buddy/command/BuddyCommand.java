package de.gurki.buddy.command;

import de.gurki.buddy.util.Graph;
import de.gurki.buddy.util.UnionFind;
import de.gurki.buddy.util.Geode;

import net.minecraft.server.command.CommandManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
// import net.minecraft.util.math.Direction.Axis;
// import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
// import net.minecraft.predicate.NumberRange.IntRange;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Direction;
import net.minecraft.block.entity.StructureBlockBlockEntity;
import net.minecraft.block.entity.BlockEntity;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.gurki.buddy.util.Clusters;
import de.gurki.buddy.util.Constants;
import de.gurki.buddy.util.Utility;
import java.util.List;
import java.util.Map;
// import java.lang.reflect.Array;
import java.util.ArrayList;
// import java.util.Collection;
// import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;


public class BuddyCommand
{
    public static final String MOD_ID = "de.gurki.buddy";
	public static final Logger LOGGER = LogManager.getLogger( MOD_ID );

    public static Geode geode_ = new Geode();
    public static StructureBlockBlockEntity structure_ = null;


    ////////////////////////////////////////////////////////////////////////////////
    public static void register( CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated )
    {
        dispatcher.register(
            CommandManager.literal( "buddy" )
                .executes( context -> run( context, 0 ) )
                .then( CommandManager.literal( "init" )
                    .executes( context -> init( context, 5 ) )
                    .then( CommandManager.argument("range", IntegerArgumentType.integer( 0 ) )
                        .executes( context -> init( context, IntegerArgumentType.getInteger( context, "range" ) ))
                    )
                )
                .then( CommandManager.literal( "tp" ).executes( BuddyCommand::tp ))
                .then( CommandManager.literal( "fly" ).executes( BuddyCommand::fly ))
                .then( CommandManager.literal( "show" ).executes( context->setHighlight( context, true ) ))
                .then( CommandManager.literal( "hide" ).executes( context->setHighlight( context, false ) ))
                .then( CommandManager.literal( "project" ).executes( context -> run( context, 1 ) ))
                .then( CommandManager.literal( "cluster" ).executes( context -> run( context, 2 ) ))
                .then( CommandManager.literal( "connect" ).executes( context -> run( context, 3 ) ))
                .then( CommandManager.literal( "validate" ).executes( context -> run( context, 4 ) ))
                .then( CommandManager.literal( "merge" ).executes( context -> run( context, 5 ) ))
                .then( CommandManager.literal( "split" ).executes( context -> run( context, 6 ) ))
                .then( CommandManager.literal( "validateSplit" ).executes( context -> run( context, 7 ) ))
                .then( CommandManager.literal( "colorize" ).executes( context -> run( context, 8 ) ))
                .then( CommandManager.literal( "explore" ).executes( context -> run( context, -1 ) ))
        );
    }

    ////////////////////////////////////////////////////////////////////////////////
    public static int setHighlight( CommandContext<ServerCommandSource> context, boolean show ) throws CommandSyntaxException {
        geode_.setHighlight( show, context.getSource().getWorld() );
        return 1;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static int tp( CommandContext<ServerCommandSource> context ) throws CommandSyntaxException {
        context.getSource().getPlayer().teleport( context.getSource().getWorld(), -734, 14.5, -254, 90, 0 );
        return 1;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static int init( CommandContext<ServerCommandSource> context, int count ) throws CommandSyntaxException
    {
        if ( count > 0 ) {
            geode_.addClosest( context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld(), count );
        } else {
            geode_.clear( context.getSource().getWorld() );
        }

        return 1;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static int fly( CommandContext<ServerCommandSource> context ) throws CommandSyntaxException
    {
        ServerPlayerEntity player = context.getSource().getPlayer();
        BlockPos center = player.getBlockPos();
        Direction dir = player.getHorizontalFacing();
        Utility.buildMachine( center, dir.rotateYCounterclockwise(), Direction.UP, Blocks.SLIME_BLOCK, context.getSource().getWorld() );
        return 1;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static int run( CommandContext<ServerCommandSource> context, int mode ) throws CommandSyntaxException
    {
        final BlockState obsidian = Blocks.OBSIDIAN.getDefaultState();
        final BlockState yellowConcrete = Blocks.YELLOW_CONCRETE.getDefaultState();
        // final BlockState orangeConcrete = Blocks.ORANGE_CONCRETE.getDefaultState();
        // final BlockState pinkConcrete = Blocks.PINK_CONCRETE.getDefaultState();

        //  clear out area

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        World world = source.getWorld();

        if ( geode_.isEmpty() ) {
            geode_.addClosest( player.getBlockPos(), world, 5 );
        }

        BlockPos boxMin = geode_.min();
        BlockPos boxMax = geode_.max();
        Utility.clearArea( boxMin, boxMax, world );
        Utility.generateBackside( boxMin, boxMax, world );

        geode_.setHighlight( true, world );

        if ( mode == 0 ) {
            return 1;
        }

        //  project buds

        List<HashSet<BlockPos>> projs = Utility.projectBuds( boxMin, boxMax, world );

        projs.get( 0 ).forEach( p -> world.setBlockState( p, obsidian ));
        projs.get( 1 ).forEach( p -> world.setBlockState( p, obsidian ));
        projs.get( 2 ).forEach( p -> world.setBlockState( p, obsidian ));


        //  build contours

        LOGGER.info( "building contours..." );

        List<BlockPos> projX2D = projs.get( 0 ).stream().map( p -> Utility.pack( p, 0 ) ).toList();
        List<BlockPos> projY2D = projs.get( 1 ).stream().map( p -> Utility.pack( p, 1 ) ).toList();
        List<BlockPos> projZ2D = projs.get( 2 ).stream().map( p -> Utility.pack( p, 2 ) ).toList();
        List<List<BlockPos>> proj2Ds = List.of( projX2D, projY2D, projZ2D );

        List<BlockPos> contX = Clusters.buildContour( projX2D );
        List<BlockPos> contY = Clusters.buildContour( projY2D );
        List<BlockPos> contZ = Clusters.buildContour( projZ2D );
        List<List<BlockPos>> conts = List.of( contX, contY, contZ );

        final int off = Constants.kOffset;
        final int[] offs = { boxMax.getX() + off, boxMax.getY() + off, boxMax.getZ() + off };

        for ( int dim = 0; dim < 3; dim++ ) {
            final int currDim = dim;
            conts.get( dim ).forEach( p-> world.setBlockState( Utility.unpack( p, currDim, offs[ currDim ] ), yellowConcrete ));
        }

        if ( mode >= 0 && mode <= 1 ) {
            return 1;
        }


        //  compute clusters

        LOGGER.info( "computing clusters..." );

        List<ArrayList<HashSet<BlockPos>>> clustersXYZ;

        switch ( mode ) {
            case 1: clustersXYZ = conts.stream().map( Clusters::findClusters ).toList(); break;
            case 2: clustersXYZ = IntStream.range( 0, 3 ).mapToObj( dim -> Clusters.combineClusters( conts.get( dim ), proj2Ds.get( dim ), true ) ).toList(); break;
            default: clustersXYZ = IntStream.range( 0, 3 ).mapToObj( dim -> Clusters.combineClusters( conts.get( dim ), proj2Ds.get( dim ), false ) ).toList(); break;
        }

        // clustersXYZ.forEach( clust -> LOGGER.info( clust.size() ) );

        Utility.drawClustersXYZ( clustersXYZ, offs, world );

        if ( mode >= 0 && mode < 4 ) {
            return 1;
        }


        //  validate clusters

        Utility.drawValidity( clustersXYZ, offs, world );

        if ( mode >= 0 && mode < 5 ) {
            return 1;
        }


        //  merge too small clusters

        BlockState cryingObsidian = Registry.BLOCK.get( new Identifier( "minecraft", "crying_obsidian" ) ).getDefaultState();

        for ( int dim = 0; dim < 3; dim++ )
        {
            final int kDim = dim;
            final HashSet<BlockPos> unreachables = Clusters.mergeSmallClusters( clustersXYZ.get( kDim ), proj2Ds.get( kDim ) );
            final ArrayList<HashSet<BlockPos>> clusters = clustersXYZ.get( kDim );

            //  remove unreachable clusters
            ArrayList<HashSet<BlockPos>> unreachableClusters = new ArrayList<>( clustersXYZ.get( kDim ).stream()
                .filter( clust -> clust.stream().anyMatch( p -> unreachables.contains( p ) ))
                .toList()
            );

            clusters.removeAll( unreachableClusters );
            unreachables.forEach( p -> world.setBlockState( Utility.unpack( p, kDim, offs[ kDim ] ), cryingObsidian ));
        }

        Utility.drawValidity( clustersXYZ, offs, world );

        if ( mode < 6 ) {
            return 1;
        }


        //  split too large clusters

        for ( int dim = 0; dim < 3; dim++ )
        {
            final int kDim = dim;
            ArrayList<HashSet<BlockPos>> validClusters = new ArrayList<>();

            for ( HashSet<BlockPos> clust : clustersXYZ.get( kDim ) )
            {
                if ( Clusters.validate( clust ) == Clusters.Validity.Valid ) {
                    validClusters.add( clust );
                    continue;
                }

                if ( Clusters.validate( clust ) != Clusters.Validity.TooLarge ) {
                    //  e.g. looong stair case with >12 blocks but no support
                    //  either try to extend with extra block, or merge with another cluster
                    validClusters.add( clust );
                    LOGGER.error( "split: UNHANDLED CASE" );
                    continue;
                }

                validClusters.addAll( Clusters.split( clust ) );
                Utility.drawClusters( validClusters, offs, world, dim );
            }

            clustersXYZ.get( kDim ).clear();
            clustersXYZ.get( kDim ).addAll( validClusters );
        }

        if ( mode < 7 ) {
            return 1;
        }

        Utility.drawValidity( clustersXYZ, offs, world );

        if ( mode < 8 ) {
            return 1;
        }

        //  assign build block

        BlockState slime = Blocks.SLIME_BLOCK.getDefaultState();
        BlockState honey = Blocks.HONEY_BLOCK.getDefaultState();
        BlockState redConcrete = Blocks.RED_CONCRETE.getDefaultState();

        for ( int dim = 0; dim < 3; dim++ )
        {
            final int kDim = dim;
            HashSet<BlockPos> validBlocks = new HashSet<>();
            ArrayList<HashSet<BlockPos>> clusters = clustersXYZ.get( kDim );
            clusters.forEach( cluster -> cluster.forEach( validBlocks::add ) );

            Graph graph = new Graph( validBlocks );
            UnionFind uf = new UnionFind( graph );
            uf.setComponents( clusters );

            HashMap<Integer, Integer> colors = new HashMap<>();

            //  traverse clusters and cluster groups to greedily assign colors

            for ( int cid = 0; cid < uf.components.size(); cid++ )
            {
                if ( colors.containsKey( cid ) ) {
                    continue;
                }

                ArrayList<Integer> group = uf.getGroup( cid );

                for ( Integer gid : group )
                {
                    if ( colors.containsKey( gid ) ) {
                        continue;
                    }

                    //  get colors of all neighbours

                    HashSet<Integer> compCols = new HashSet<Integer>();

                    for ( Integer nid : uf.edges.get( gid ) )
                    {
                        if ( ! colors.containsKey( nid ) ) {
                            continue;
                        }

                        compCols.add( colors.get( nid ) );
                    }

                    if ( compCols.isEmpty() ) {
                        colors.put( gid, 0 );
                    } else if ( compCols.size() == 1 ) {
                        Integer otherCol = compCols.iterator().next();
                        Integer col = otherCol == 0 ? 1 : 0;
                        colors.put( gid, col );
                    } else {
                        colors.put( gid, 2 );
                        LOGGER.error( "INVALID COLORS" );
                    }
                }

                //  swap colors to maximize 0-use

                int[] counts = new int[ 3 ];
                group.forEach( gid -> counts[ colors.get( gid ) ] += uf.components.get( gid ).size() );

                if ( counts[ 0 ] >= counts[ 1 ] ) {
                    continue;
                }

                group.forEach( gid -> {
                    int newCol = ( colors.get( gid ) == 0 ) ? 1 : 0;
                    colors.put( gid, newCol );
                });
            }

            //  colorize

            for ( Map.Entry<Integer, Integer> entry : colors.entrySet() )
            {
                Integer col = entry.getValue();
                HashSet<Integer> comp = uf.components.get( entry.getKey() );

                BlockState state = ( col == 0 ) ? slime : ( col == 1 ) ? honey : redConcrete;

                for ( Integer vid : comp ) {
                    BlockPos pos = graph.verts[ vid ];
                    world.setBlockState( Utility.unpack( pos, kDim, offs[ kDim ] ), state );
                }
            }
        }

        return 1;
    }
}
