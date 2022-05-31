# Amethyst Buddy

Fabric mod for automatic amethyst farm generation.

> This mod **will** alter your world. Use with caution.

- This mod is _very much_ work in progress
- There's a bunch of bugs and unhandled corner cases
- Usability could use some love
- Code base could use a _lot_ of love


## Setting

The farm operates by pushing flying machines through the cluster from all directions, harvesting the amethysts along the way.<br>
These flying machine have the following constraints.

- push limit of 12 blocks
- are 3 blocks wide or high
- need an extra immovable block as stopper

This poses an interesting challenge to generate an optimal pattern of honey and slime blocks, that

- fulfill all of above constraints
- minimize number of flying machines
- maximize farm efficiency

This is a very hard problem (NP-complete), which is why I tried to automate it.
Spoiler: it's tough... 😅


## Overview

Use the `buddy` command as follows.

- run `buddy colorize` for the full projection and cluster computation
- fix any issues you see manually by placing or replacing honey and slime blocks
- run `buddy machines` to read the current layout and generate flying machines

The `buddy colorize` command can also be run in stages up to a certain point.
This is mostly for debugging purposes and the curious.
Here's a step by step guide.


## Guide

### 0. Find your Geode
<image src="screenshots/guide/00_geode.png" width=480>


### 1. Set Bounding Box
**`buddy add`** smartly searches for the nearest geode in a 16-block radius around the player.
Starting from a seed `Budding Amethyst`, it searches outwards `range` blocks and extends the bounding box every time it finds another one.
The default  `range` is `5`.

You can add multiple geodes to your bounding box by moving closer to a neighbouring node and repeating the `buddy add` command.

You can clear the bounding box using `buddy clear`.

You can show or hide the current bounding box using `buddy show` and `buddy hide`.
The bounding box can also be cleared by breaking the structure block at the far down corner.

<image src="screenshots/guide/01_add.png" width=480>


### 2. Project and Mark
**`buddy project`** projects all budding amethysts in the set bounding box to the cardinal planes.
It also marks all the necessary blocks to cover.

<image src="screenshots/guide/02_project.png" width=480>


### 3. Cluster
**`buddy cluster`** finds and connects isolated blocks that can be joined to some cluster by adding a single block.

<image src="screenshots/guide/03_cluster.png" width=480>


### 4. Connect
**`buddy connect`** finds and connects invalid clusters that can be joined by adding a single block.

<image src="screenshots/guide/04_connect.png" width=480>


### 5. Validate Connect
**`buddy validateConnect`** visualizes the current validity of each cluster.
- **Pink**: too small
- **Magenta**: too large
- **Purple**: no support

<image src="screenshots/guide/05_validateConnect.png" width=480>


### 6. Merge
**`buddy merge`** merges all clusters that are too small with neighbouring ones.

<image src="screenshots/guide/06_merge.png" width=480>


### 7. Split
**`buddy split`** splits all clusters that are too large, trying to comply to all constraints.

<image src="screenshots/guide/07_split.png" width=480>


### 8. Validate Split
**`buddy validateSplit`** visualizes the current validity of each cluster.

<image src="screenshots/guide/08_validateSplit.png" width=480>


### 9. Colorize
**`buddy colorize`** tries to assign valid honey and slime blocks to the clusters.

<image src="screenshots/guide/09_colorize.png" width=480>


### 10. Machines (Mark Only)
**`buddy machines true`** visualizes the supports where flying machines would be generated.

<image src="screenshots/guide/10_machines-mark.png" width=480>


### 11. Machines
**`buddy machines`** actually generates the flying machines.

<image src="screenshots/guide/11_machines.png" width=480>


## Backlog
- check for and remove [superfluent blocks](screenshots/2022-06-01_00.04.50.png) after merge
- avoid choosing supports with conflicts
  - not adjacent to [other block type](screenshots/2022-06-01_00.38.21.png)
  - not adjacent to any [other support](screenshots/2022-05-31_23.10.49.png)
- handle [infinite split](screenshots/2022-05-29_21.10.43.png)
- handle [uncolorable](screenshots/2022-05-30_16.17.51.png) configurations