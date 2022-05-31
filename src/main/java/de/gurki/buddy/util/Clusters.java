package de.gurki.buddy.util;

import net.minecraft.util.math.BlockPos;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;


public class Clusters
{
    public static final String MOD_ID = "de.gurki.buddy";
	public static final Logger LOGGER = LogManager.getLogger( MOD_ID );

    public enum Validity {
        Valid,
        TooSmall,
        TooLarge,
        NoStraight
    };


    ////////////////////////////////////////////////////////////////////////////////
    public static Set<Integer> neighbouringClusters( BlockPos p, ArrayList<HashSet<BlockPos>> clusters )
    {
        Set<Integer> cids = new HashSet<Integer>();
        List<BlockPos> opts = List.of( p.up(), p.east(), p.down(), p.west() );

        for ( BlockPos opt : opts )
        {
            for ( var cid = 0; cid < clusters.size(); cid++ )
            {
                if ( ! clusters.get( cid ).contains( opt ) ) {
                    continue;
                }

                cids.add( cid );
            }
        }

        return cids;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static int combinedSize( Set<Integer> cids, ArrayList<HashSet<BlockPos>> clusters ) {
        return cids.stream().mapToInt( cid -> clusters.get( cid ).size() ).reduce( 0, Integer::sum );
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static ArrayList<HashSet<BlockPos>> combineClusters( List<BlockPos> contour, List<BlockPos> projX2D, boolean singleOnly )
    {
        HashSet<BlockPos> buds = new HashSet<BlockPos>( projX2D );
        ArrayList<HashSet<BlockPos>> clusters = findClusters( contour );

        boolean anyMerge = true;

        while ( anyMerge )
        {
            anyMerge = false;
            clusters.sort( ( c1, c2 ) -> c1.size() - c2.size() );   //  connect smallest components first

            //  look for cluster that can be connected by single block
            for ( int currId = 0; currId < clusters.size(); currId++ )
            {
                //  explore surroundings of current cluster
                HashSet<BlockPos> currCluster = clusters.get( currId );

                for ( BlockPos p : currCluster )
                {
                    List<BlockPos> opts = List.of( p.up(), p.east(), p.down(), p.west() );
                    boolean couldMerge = false;

                    for ( BlockPos opt : opts )
                    {
                        if ( buds.contains( opt ) || currCluster.contains( opt ) ) {
                            continue;
                        }

                        //  try to insert point opt and merge neighbouring clusters
                        Set<Integer> cids = neighbouringClusters( opt, clusters );

                        if ( singleOnly ? cids.size() != 2 : cids.size() <= 1 ) {
                            continue;
                        }

                        final int count = 1 + combinedSize( cids, clusters );

                        if ( count > 12 ) {
                            continue;
                        }

                        final int kCurrId = currId;
                        List<HashSet<BlockPos>> mergeClusters = cids.stream().filter( id -> id != kCurrId ).map( id -> clusters.get( id ) ).toList();
                        currCluster.add( opt );

                        for ( HashSet<BlockPos> mergeCluster : mergeClusters ) {
                            currCluster.addAll( mergeCluster );
                            clusters.remove( mergeCluster );
                        }

                        couldMerge = true;
                        break;
                    }

                    if ( couldMerge ) {
                        anyMerge = true;
                        break;
                    }
                }

                //  restart as indices have shifted
                if ( anyMerge ) {
                    break;
                }
            }
        }

        return clusters;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static List<BlockPos> buildContour( List<BlockPos> points )
    {
        if ( points.isEmpty() ) {
            return null;
        }

        Set<BlockPos> contour = new HashSet<BlockPos>();

        for ( BlockPos p : points ) {
            contour.add( p.up() );
            contour.add( p.down() );
            contour.add( p.east() );
            contour.add( p.west() );
        }

        return contour.stream().filter( p -> ! points.contains( p ) ).toList();
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static ArrayList<HashSet<BlockPos>> findClusters( List<BlockPos> points )
    {
        if ( points.isEmpty() ) {
            return null;
        }

        ArrayList<HashSet<BlockPos>> clusters = new ArrayList<>();

        for ( BlockPos p : points )
        {
            ArrayList<HashSet<BlockPos>> currClusters = new ArrayList<>();

            //  find clusters c that touch point p
            for ( HashSet<BlockPos> c : clusters )
            {
                boolean hasCluster = false;

                //  closest point q in cluster c
                for ( BlockPos q : c )
                {
                    if ( p.getManhattanDistance( q ) != 1 ) {
                        continue;
                    }

                    hasCluster = true;
                    break;
                }

                if ( ! hasCluster ) {
                    continue;
                }

                currClusters.add( c );
            }

            //  create new cluster [p]
            if ( currClusters.isEmpty() )
            {
                HashSet<BlockPos> newCluster = new HashSet<>();
                newCluster.add( p );
                clusters.add( newCluster );

                continue;
            }

            //  merge all clusters neighbouring point p
            currClusters.get( 0 ).add( p );

            for ( var i = 1; i < currClusters.size(); i++ ) {
                currClusters.get( 0 ).addAll( currClusters.get( i ) );
                clusters.remove( currClusters.get( i ) );
            }
        }

        return clusters;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static Validity validate( HashSet<BlockPos> cluster )
    {
        if ( cluster.size() < 4 ) {
            return Validity.TooSmall;
        }

        if ( cluster.size() > 12 ) {
            return Validity.TooLarge;
        }

        boolean hasSupport = false;

        for ( BlockPos pos : cluster )
        {
            hasSupport = ! Support.findConnectedSupports( cluster, pos ).isEmpty();

            if ( hasSupport ) {
                break;
            }
        }

        if ( ! hasSupport ) {
            return Validity.NoStraight;
        }

        return Validity.Valid;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static ArrayList<HashSet<BlockPos>> split( HashSet<BlockPos> cluster )
    {
        LOGGER.info( "--- split ---" );

        //  base case if cluster is already small enough
        if ( cluster.size() <= 12 ) {
            ArrayList<HashSet<BlockPos>> groups = new ArrayList<>();
            groups.add( cluster );
            groups.add( new HashSet<BlockPos>() );
            return groups;
        }


        //  init split with farthest stable supports

        int targetCount = (int)Math.ceil( cluster.size() / 12.0 );
        int targetSize = (int)Math.ceil( cluster.size() / (double)targetCount );

        Graph graph = new Graph( cluster );
        UnionFind uf = new UnionFind( graph );

        Integer[] stableIds = graph.getStablePair( uf.components.get( 0 ) );    //  graph vertex ids; single cluster at init
        HashSet<Integer> clustA = Support.findClosestSupport( cluster, graph, stableIds[ 0 ] ).getIndices( graph );
        HashSet<Integer> clustB = Support.findClosestSupport( cluster, graph, stableIds[ 1 ] ).getIndices( graph );
        Integer repA = clustA.iterator().next();
        Integer repB = clustB.iterator().next();
        HashSet<Integer> reps = new HashSet<>( List.of( repA, repB ) );

        LOGGER.info( "processing " + repA + " / " + repB );

        uf.extract( clustA );
        uf.extract( clustB );
        LOGGER.info( uf.components );
        // LOGGER.info( uf.edges );

        //  merge detached components
        uf.mergeIsolated( repA, reps );
        uf.mergeIsolated( repB, reps );
        LOGGER.info( uf.components );
        // LOGGER.info( uf.edges );

        HashSet<Integer> next = new HashSet<>();
        next.add( repA );
        next.add( repB );


        //  expand clusters

        final int kSanityLimit = 24;
        int sanityExit = 0;

        while ( ! next.isEmpty() && sanityExit++ < kSanityLimit )
        {
            //  expand smallest cluster
            Integer rep = next.stream().sorted( ( a, b ) -> uf.find( a ).size() - uf.find( b ).size() ).findFirst().get();
            Integer cid = uf.ids.get( rep );
            Iterator<Integer> iter = uf.iterateAdjacent( cid, reps ).iterator();

            // List<Integer> adjs = new ArrayList<>();
            // uf.iterateAdjacent( cid ).forEach( adjs::add );
            // LOGGER.info( adjs );
            LOGGER.info( "process " + rep );

            while ( iter.hasNext() && next.contains( rep ) )
            {
                if ( uf.components.get( cid ).size() >= targetSize && targetCount > 2 ) {
                    //  finalize if large enough and no more expansion needed
                    LOGGER.info( "finalize " + rep );
                    next.remove( rep );
                    break;
                }

                Integer vid = iter.next();
                LOGGER.info( "expand " + rep + " " + vid );

                HashMap<Integer, Integer> prevIds = new HashMap<Integer, Integer>( uf.ids );
                // LOGGER.info( prevIds );

                uf.relocate( vid, rep );
                LOGGER.info( uf.components );
                // LOGGER.info( uf.edges );

                if ( uf.components.size() == 2 ) {
                    //  reached base case, can exit
                    next.clear();
                    LOGGER.info( "base exit" );
                    break;
                }

                uf.mergeIsolated( rep, reps );
                LOGGER.info( uf.components );
                // LOGGER.info( uf.edges );
                // LOGGER.info( prevIds );

                final int currSize = uf.find( rep ).size();

                if ( currSize > 12 ) {
                    //  expanded too far, revert merge
                    uf.ids = prevIds;
                    uf.updateComponents();
                    LOGGER.info( "revert" );
                    LOGGER.info( uf.components );
                    //  try again by merging another adjacent block, if any
                    continue;
                }

                //  continue by choosing current smallest cluster anew
                break;
            }

            if ( uf.components.size() == targetCount ) {
                LOGGER.info( "target exit" );
                break;
            }
        }

        if ( sanityExit >= kSanityLimit ) {
            LOGGER.error( "something went wrong" );
        }


        //  build block clusters

        final ArrayList<HashSet<BlockPos>> split = new ArrayList<>();

        for ( HashSet<Integer> comp : List.of( uf.find( repA ), uf.find( repB ) ) ) {
            split.add( new HashSet<BlockPos>(
                comp.stream().map( v -> graph.verts[ v ] ).toList() )
            );
        }


        //  split any leftover component recursively

        uf.components.stream().filter( comp -> comp.size() > 12 ).forEach( comp ->
        {
            HashSet<BlockPos> poss = new HashSet<>();
            for ( Integer vid : comp ) {
                poss.add( graph.verts[ vid ] );
            }

            ArrayList<HashSet<BlockPos>> remnants = split( poss );
            split.addAll( remnants );
        });

        return split;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static HashSet<BlockPos> mergeSmallClusters( ArrayList<HashSet<BlockPos>> clusters, List<BlockPos> buds )
    {
        boolean anyMerge = true;
        HashSet<BlockPos> unreachables = new HashSet<>();
        HashSet<HashSet<BlockPos>> unreachableClusters = new HashSet<>();

        while ( anyMerge )
        {
            anyMerge = false;

            for ( int cid = 0; cid < clusters.size(); cid++ )
            {
                HashSet<BlockPos> clust = clusters.get( cid );
                final Clusters.Validity validity = Clusters.validate( clust );
                final boolean isValid = ( validity != Clusters.Validity.TooLarge ) && ( validity != Clusters.Validity.Valid );

                if ( ! isValid ) {
                    continue;
                }

                //  find bridge with smallest post-merge size
                int minCount = Integer.MAX_VALUE;
                BlockPos minPos = null;
                Set<Integer> minCids = null;

                for ( BlockPos p : clust )
                {
                    List<BlockPos> opts = List.of( p.up(), p.east(), p.down(), p.west() );

                    for ( BlockPos opt : opts )
                    {
                        if ( clust.contains( opt ) || buds.contains( opt ) ) {
                            continue;
                        }

                        Set<Integer> cids = Clusters.neighbouringClusters( opt, clusters );
                        final int count = Clusters.combinedSize( cids, clusters );

                        if ( count < minCount && count > clust.size() ) {
                            minCount = count;
                            minPos = opt;
                            minCids = cids;
                        }
                    }
                };

                //  unreachable cluster
                if ( minCount == Integer.MAX_VALUE ) {
                    unreachables.addAll( clust );
                    unreachableClusters.add( clust );
                    continue;
                }

                //  add minPos and merge clusters
                List<HashSet<BlockPos>> mergeClusters = minCids.stream().map( id -> clusters.get( id ) ).filter( c -> c != clust ).toList();
                clust.add( minPos );

                for ( HashSet<BlockPos> mergeCluster : mergeClusters ) {
                    clust.addAll( mergeCluster );
                    clusters.remove( mergeCluster );
                }

                anyMerge = true;
                break;
            };

            clusters.remove( unreachableClusters );
        }

        return unreachables;
    }
}
