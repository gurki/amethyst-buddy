package de.gurki.buddy.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.AbstractIterator;


//  union-find that takes graph connectivity into account
public class UnionFind
{
    public final Graph graph;
    public ArrayList<HashSet<Integer>> components;    // [{vert}]
    public HashMap<Integer, Integer> ids;  //  vert -> comp
    public HashMap<Integer, HashSet<Integer>> edges;   // comp -> {comp}


    ////////////////////////////////////////////////////////////////////////////////
    public UnionFind( Graph graph )
    {
        this.graph = graph;
        this.components = new ArrayList<>();
        this.ids = new HashMap<>();
        this.edges = new HashMap<>();

        initComponents();
    }


    ////////////////////////////////////////////////////////////////////////////////
    public void initComponents()
    {
        components.clear();
        final HashSet<Integer> done = new HashSet<>();

        for ( int i = 0; i < graph.verts.length; i++ )
        {
            if ( done.contains( i ) ) {
                continue;
            }

            final HashSet<Integer> cluster = new HashSet<>();
            cluster.add ( i );
            ids.put( i, components.size() );

            for ( Integer vid : graph.iterateBFS( i ) ) {
                cluster.add( vid );
                done.add( vid );
                ids.put( vid, components.size() );
            }

            components.add( cluster );
        }

        updateEdges();
    }


    ////////////////////////////////////////////////////////////////////////////////
    public HashSet<Integer> find( Integer vid ) {
        return components.get( ids.get( vid ) );
    }


    ////////////////////////////////////////////////////////////////////////////////
    public Integer findRep( Integer cid, Integer vid ) {
        return components.get( cid ).stream().dropWhile( i -> i == vid ).findFirst().get();
    }


    ////////////////////////////////////////////////////////////////////////////////
    public void extract( HashSet<Integer> vids )
    {
        final HashSet<Integer> comp = new HashSet<Integer>();

        for ( Integer vid : vids ) {
            final Integer cid = ids.get( vid );
            components.get( cid ).remove( vid );
            comp.add( vid );
            ids.put( vid, components.size() );
        }

        components.add( comp );
        updateComponents();
    }


    ////////////////////////////////////////////////////////////////////////////////
    public void extract( Integer vid ) {
        extract( new HashSet<Integer>( List.of( vid ) ));
    }


    ////////////////////////////////////////////////////////////////////////////////
    public void updateEdges()
    {
        edges.clear();

        for ( int cid = 0; cid < components.size(); cid++ )
        {
            final HashSet<Integer> comp = components.get( cid );
            final HashSet<Integer> neighs = new HashSet<>();

            for ( Integer vid : comp ) {
                for ( Integer nid : graph.edges.get( vid ) ) {
                    final Integer ncid = ids.get( nid );
                    if ( ncid == cid ) continue;
                    neighs.add( ncid );
                }
            }

            edges.put( cid, neighs );
        }
    }


    ////////////////////////////////////////////////////////////////////////////////
    public void merge( Integer cA, Integer cB )
    {
        for ( Integer vB : components.get( cB ) ) {
            ids.put( vB, cA );
        }

        components.get( cA ).addAll( components.get( cB ) );
        components.get( cB ).clear();
        updateComponents();
    }


    ////////////////////////////////////////////////////////////////////////////////
    public void mergeIsolated( Integer rep, HashSet<Integer> ignoreReps )    //  vertex representing the component
    {
        Integer cA = ids.get( rep );
        HashSet<Integer> comp = edges.get( cA );
        HashSet<Integer> ignoreComps = new HashSet<>( ignoreReps.stream().map( ids::get ).toList() );

        for ( Integer cB : comp )
        {
            if ( ignoreComps.contains( cB ) ) {
                continue;
            }

            if ( edges.get( cB ).size() > 1 ) {
                continue;
            }

            HashSet<Integer> neighs = components.get( cB );

            for ( Integer vid : neighs ) {
                ids.put( vid, cA );
            }

            components.get( cA ).addAll( neighs );
            components.get( cB ).clear();
        }

        updateComponents();
    }


    ////////////////////////////////////////////////////////////////////////////////
    public void relocate( Integer vert, Integer toVert )
    {
        Integer fromComp = ids.get( vert );
        Integer toComp = ids.get( toVert );
        components.get( fromComp ).remove( vert );
        components.get( toComp ).add( vert );
        ids.put( vert, toComp );
        updateComponents();
    }


    ////////////////////////////////////////////////////////////////////////////////
    public void updateComponents()
    {
        final HashSet<Integer> done = new HashSet<>();
        final ArrayList<HashSet<Integer>> newComps = new ArrayList<>();

        for ( int i = 0; i < graph.verts.length; i++ )
        {
            if ( done.contains( i ) ) {
                continue;
            }

            final Integer cid = ids.get( i );
            final HashSet<Integer> comp = new HashSet<>();
            comp.add ( i );

            for ( Integer vid : graph.iterateBFS( i, id -> ids.get( id ) == cid ) ) {
                comp.add( vid );
                done.add( vid );
            }

            newComps.add( comp );
        }

        newComps.sort( ( cA, cB ) -> cA.size() - cB.size() );
        ids.clear();

        for ( int cid = 0; cid < newComps.size(); cid++ ) {
            HashSet<Integer> comp = newComps.get( cid );
            for ( Integer vid : comp ) {
                ids.put( vid, cid );
            }
        }

        components = newComps;
        updateEdges();
    }


    ////////////////////////////////////////////////////////////////////////////////
    public Iterable<Integer> iterateAdjacent( Integer cid, HashSet<Integer> ignoreReps )
    {
        if ( cid < 0 || cid >= components.size() ) {
            return null;
        }

        HashSet<Integer> ignoreComps = new HashSet<>( ignoreReps.stream().map( ids::get ).toList() );

        return () -> new AbstractIterator<Integer>()
        {
            private Queue<Integer> next = new LinkedList<>();
            {
                HashSet<Integer> nids = new HashSet<>();

                for ( Integer vid : components.get( cid ) )
                {
                    for ( Integer nid : graph.edges.get( vid ) )
                    {
                        Integer ncid = ids.get( nid );

                        if ( cid == ncid || nids.contains( nid ) || ignoreComps.contains( ncid ) ) {
                            continue;
                        }

                        nids.add( nid );
                        this.next.add( nid );
                    }
                }
            }

            @Override
            protected Integer computeNext()
            {
                if ( this.next.isEmpty() ) {
                    return (Integer)this.endOfData();
                }

                return this.next.poll();
            }
        };
    }
}
