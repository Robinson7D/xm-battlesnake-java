/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlesnake;

import com.battlesnake.data.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.Random;

@RestController
public class RequestController2 {

  Logger logger = LoggerFactory.getLogger(RequestController2.class);

  private double FOOD_WEIGHT = 1;
  private double OPEN_SPACE_WEIGHT = 1.25;
  private double ENEMY_HEAD_PREDICTION_WEIGHT = 0.8;

  private String[] taunts = { "Life is not a malfunction.", "Attractive. Nice software. Hmmmm.",
      "Hey, laser lips, your mama was a snow blower.", "Number 5 is alive.", "Ho ho ho ho ho ho ho ho ho ho ho!",
      "Hee hee hee hee hee hee hee hee hee!", "Nyuk, nyuk nyuk nyuk nyuk nyuk nyuk nyuk nyuk!",
      "Program say to kill, to disassemble, to make dead. Number 5 cannot.", "Verdict: starvation to death.",
      "Snake. Target. Nevermore.", "No disassemble Number Five!", "Number 5 stupid name... want to be Troy or Nick!",
      "Frankie, you broke the unwritten law.", "Come on, treads, don't fail me now!", };

  private int currentTaunt = 0;

  @RequestMapping(value = "/start", method = RequestMethod.POST, produces = "application/json")
  public StartResponse start(@RequestBody StartRequest request) {
    return new StartResponse().setName("Jonny 5").setColor("#04B626")
        .setHeadUrl("https://66.media.tumblr.com/tumblr_m9yt1tExMO1qkpvmmo1_400.jpg").setHeadType(HeadType.DEAD)
        .setTailType(TailType.PIXEL).setTaunt("Waiting...");
  }

  @RequestMapping(value = "/move", method = RequestMethod.POST, produces = "application/json")
  public MoveResponse move(@RequestBody MoveRequest request) {
    MoveResponse moveResponse = new MoveResponse();

    // change taunt every 20 moves
    if (request.getTurn() % 20 == 0) {
      Random rand = new Random();
      currentTaunt = rand.nextInt(taunts.length - 1);
    }
    Snake mySnake = findOurSnake(request); // kind of handy to have our snake at this level
    int[] head = mySnake.getCoords()[0];

    double[][] map = getMap(request);

    // List<Move> towardsFoodMoves = moveTowardsFood(request,
    // mySnake.getCoords()[0]);

    // if (towardsFoodMoves != null && !towardsFoodMoves.isEmpty()) {
    // return moveResponse.setMove(towardsFoodMoves.get(0)).setTaunt("I'm hungry");
    // } else {
    // return moveResponse.setMove(Move.DOWN).setTaunt("Oh Drat");
    // }

    logger.info(Arrays.deepToString(map));
    return moveResponse.setMove(getMove(request, mySnake, map, head)).setTaunt(taunts[currentTaunt]);
  }

  @RequestMapping(value = "/end", method = RequestMethod.POST)
  public Object end() {
    // No response required
    Map<String, Object> responseObject = new HashMap<String, Object>();
    return responseObject;
  }

  Move getMove(MoveRequest request, Snake mySnake, double[][] map, int[] head) {
    double topScore = -100000;
    Move move = Move.UP;

    for (Move thisMove : Move.values()) {
      double score = getScore(map, head, thisMove);
      logger
          .info("<><><><> MOVE: " + thisMove.getName() + " has score: " + score + " TOP SCORE CURRENTLY: " + topScore);
      if (score > topScore) {
        topScore = score;
        move = thisMove;
        logger.info("<><><><> NEW TOP SCORE! " + thisMove.getName());
      }
    }
    logger.info("<><><><> Final preference: " + move.getName());
    return move;
  }

  double[][] getMap(MoveRequest request) {
    int width = request.getWidth();
    int height = request.getHeight();
    int area = width * height;

    // Initialize based on food:
    double[][] map = new double[width][height];
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        double foodScore = Math.abs((area - getDistanceFromFood(request, x, y)) / area);
        map[x][y] = 1 + (foodScore * FOOD_WEIGHT);
      }
    }

    Snake mySnake = findOurSnake(request);

    // Don't hit snakes
    for (Snake snake : request.getSnakes()) {
      int [][] coords = snake.getCoords();

      for (int i = 0; i <coords.length; i++) {
          int[] p = coords[i];
          if (!(snake.getId() == request.getYou() && i == coords.length - 1)) {
              map[p[0]][p[1]] = 0;
          }
      }

      if (snake != mySnake) {
          updateMapForEnemyHeads(map, snake, mySnake);
      }
    }

    // Add score based on open space:
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (map[x][y] > 0) {
          map[x][y] += (getDistanceFromWall(map, x, y) * OPEN_SPACE_WEIGHT);
        }
      }
    }

    return map;
  }

    void updateMapForEnemyHeads(double[][] map, Snake enemy, Snake us) {
        boolean wereLarger = us.getCoords().length > enemy.getCoords().length;
        double modifier = ENEMY_HEAD_PREDICTION_WEIGHT * (wereLarger ? 1 : -1);

        int [] enemyHead = enemy.getCoords()[0];
        if (enemyHead[0] > 0) {
            map[enemyHead[0] - 1][enemyHead[1]] *= modifier;
        }
        if (enemyHead[0] < map.length - 1) {
            map[enemyHead[0] + 1][enemyHead[1]] *= modifier;
        }
        if (enemyHead[1] > 0) {
            map[enemyHead[0]][enemyHead[1] - 1] *= modifier;
        }
        if (enemyHead[1] < map[0].length - 1) {
            map[enemyHead[0]][enemyHead[1] + 1] *= modifier;
        }
    }

  double getScore(double[][] map, int[] head, Move move) {
    if (move == Move.LEFT) {
      return (head[0] > 0) ? map[head[0] - 1][head[1]] : 0;
    }
    if (move == Move.RIGHT) {
      return (head[0] < map.length - 1) ? map[head[0] + 1][head[1]] : 0;
    }
    if (move == Move.DOWN) {
      return (head[1] < map[0].length - 1) ? map[head[0]][head[1] + 1] : 0;
    }
    // UP
    return (head[1] > 0) ? map[head[0]][head[1] - 1] : 0;

  }

  boolean moveIsOk(int[][] map, int[] head, Move move) {
    if (move == Move.LEFT) {
      return head[0] > 0 && map[head[0] - 1][head[1]] > 0;
    }
    if (move == Move.RIGHT) {
      return head[0] < map.length - 1 && map[head[0] + 1][head[1]] > 0;
    }
    if (move == Move.DOWN) {
      return head[1] < map[0].length - 1 && map[head[0]][head[1] + 1] > 0;
    }
    // UP
    return head[1] > 0 && map[head[0]][head[1] - 1] > 0;
  }

  Move getValidMove(int[][] map, int[] head) {
    if (head[0] > 0 && map[head[0] - 1][head[1]] > 0) {
      return Move.LEFT;
    }
    if (head[0] < map.length - 1 && map[head[0] + 1][head[1]] > 0) {
      return Move.RIGHT;
    }
    if (head[1] < map[0].length - 1 && map[head[0]][head[1] + 1] > 0) {
      return Move.DOWN;
    }
    // UP
    return Move.UP;
  }

  /*
   * Go through the snakes and find your team's snake
   *
   * @param request The MoveRequest from the server
   * 
   * @return Your team's snake
   */
  private Snake findOurSnake(MoveRequest request) {
    String myUuid = request.getYou();
    List<Snake> snakes = request.getSnakes();
    return snakes.stream().filter(thisSnake -> thisSnake.getId().equals(myUuid)).findFirst().orElse(null);
  }

  public double getDistanceFromFood(MoveRequest request, int x, int y) {
    int[] firstFoodLocation = request.getFood()[0];
    return Math.abs(firstFoodLocation[0] - x) + Math.abs(firstFoodLocation[1] - y);
  }

  public double getDistanceFromWall(double[][] map, int itemX, int itemY) {
    int width = map.length;
    int height = map[0].length;
    int area = width * height;
    int largestDistanceX = 0;
    int largestDistanceY = 0;

    for (int i = 1; i < itemX; i++) {
      int x = itemX - i;
      if (map[x][itemY] == 0) {
        break;
      } else if (i > largestDistanceX) {
        largestDistanceX = i;
      }
    }
    for (int x = itemX + 1; x < width - 1; x++) {
      if (map[x][itemY] == 0) {
        break;
      }
      int dist = x - itemX;
      if (dist > largestDistanceX) {
        largestDistanceX = dist;
      }
    }

    for (int i = 1; i < itemY; i++) {
      int y = itemY - i;
      if (map[itemX][y] == 0) {
        break;
      } else if (i > largestDistanceY) {
        largestDistanceY = i;
      }
    }
    for (int y = itemY + 1; y < height - 1; y++) {
      if (map[itemX][y] == 0) {
        break;
      }
      int dist = y - itemY;
      if (dist > largestDistanceY) {
        largestDistanceY = dist;
      }
    }

    return (largestDistanceX * largestDistanceY) / area;
  }

  /*
   * Simple algorithm to find food
   *
   * @param request The MoveRequest from the server
   * 
   * @param request An integer array with the X,Y coordinates of your snake's head
   * 
   * @return A Move that gets you closer to food
   */
  public ArrayList<Move> moveTowardsFood(MoveRequest request, int[] mySnakeHead) {
    ArrayList<Move> towardsFoodMoves = new ArrayList<>();

    int[] firstFoodLocation = request.getFood()[0];

    if (firstFoodLocation[0] < mySnakeHead[0]) {
      towardsFoodMoves.add(Move.LEFT);
    }

    if (firstFoodLocation[0] > mySnakeHead[0]) {
      towardsFoodMoves.add(Move.RIGHT);
    }

    if (firstFoodLocation[1] < mySnakeHead[1]) {
      towardsFoodMoves.add(Move.UP);
    }

    if (firstFoodLocation[1] > mySnakeHead[1]) {
      towardsFoodMoves.add(Move.DOWN);
    }

    return towardsFoodMoves;
  }

}