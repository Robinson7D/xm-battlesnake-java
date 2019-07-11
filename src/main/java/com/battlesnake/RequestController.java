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
import org.springframework.web.bind.annotation.*;

//@RestController
public class RequestController {

    int[][] map;

    @RequestMapping(value="/start", method=RequestMethod.POST, produces="application/json")
    public StartResponse start(@RequestBody StartRequest request) {
        return new StartResponse()
                .setName("Jonny 5")
                .setColor("#04B626")
                .setHeadUrl("http://vignette1.wikia.nocookie.net/nintendo/images/6/61/Bowser_Icon.png/revision/latest?cb=20120820000805&path-prefix=en")
                .setHeadType(HeadType.SMILE)
                .setTailType(TailType.SKINNYTAIL)
                .setTaunt("Number 5 is alive!");
    }

    @RequestMapping(value="/move", method=RequestMethod.POST, produces = "application/json")
    public MoveResponse move(@RequestBody MoveRequest request) {
        MoveResponse moveResponse = new MoveResponse();
        
        Snake mySnake = findOurSnake(request); // kind of handy to have our snake at this level
        int[] head = mySnake.getCoords()[0];

        map = getMap(request);

//        List<Move> towardsFoodMoves = moveTowardsFood(request, mySnake.getCoords()[0]);
        
//        if (towardsFoodMoves != null && !towardsFoodMoves.isEmpty()) {
//            return moveResponse.setMove(towardsFoodMoves.get(0)).setTaunt("I'm hungry");
//        } else {
//            return moveResponse.setMove(Move.DOWN).setTaunt("Oh Drat");
//        }

//        Log.info(map);

            return moveResponse.setMove(getMove(request, mySnake, map, head)).setTaunt("???");
    }

    @RequestMapping(value="/end", method=RequestMethod.POST)
    public Object end() {
        // No response required
        Map<String, Object> responseObject = new HashMap<String, Object>();
        return responseObject;
    }

    Move getMove(MoveRequest request, Snake mySnake, int[][] map, int[] head) {
        Move foodMove = moveTowardsFood(request, mySnake.getCoords()[0]);
        if (moveIsOk(map, head, foodMove)) {

        }

//        for (Move foodMove : foodMoves) {
//            if (moveIsOk(map, head, foodMove))
//                return foodMove;
//        }

        for (Move move : Move.values()) {
            if (moveIsOk(map, head, move)) {
                return move;
            }
        }

        return Move.UP;
    }

    int[][] getMap(MoveRequest request) {
        int[][] map = new int[request.getWidth()][request.getHeight()];
        for (int x = 0; x < request.getWidth(); x++) {
            for (int y = 0; y < request.getHeight(); y++) {
                map[x][y] = 1;
            }
        }

        // Don't hit snakes
        for (Snake snake : request.getSnakes()) {
            for (int i = 0; i < snake.getCoords().length; i++) {
                int[] p = snake.getCoords()[i];
                map[p[0]][p[1]] = 0;
            }
        }

        return map;
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

    /*
     *  Go through the snakes and find your team's snake
     *  
     *  @param  request The MoveRequest from the server
     *  @return         Your team's snake
     */
    private Snake findOurSnake(MoveRequest request) {
        String myUuid = request.getYou();
        List<Snake> snakes = request.getSnakes();
        return snakes.stream().filter(thisSnake -> thisSnake.getId().equals(myUuid)).findFirst().orElse(null);
    }

    /*
     *  Simple algorithm to find food
     *  
     *  @param  request The MoveRequest from the server
     *  @param  request An integer array with the X,Y coordinates of your snake's head
     *  @return         A Move that gets you closer to food
     */    
    public Move moveTowardsFood(MoveRequest request, int[] mySnakeHead) {
        ArrayList<Move> towardsFoodMoves = new ArrayList<>();

        int[] firstFoodLocation = request.getFood()[0];

        int diffx = mySnakeHead[0] - firstFoodLocation[0];
        int diffy = mySnakeHead[1] - firstFoodLocation[1];

        Move hmove = (diffx > 0) ? Move.LEFT : Move.RIGHT;
        Move vmove = (diffy > 0) ? Move.UP : Move.DOWN;

        Move preferred = Math.abs(diffx) > Math.abs(diffy) ? hmove : vmove;

        return preferred;
    }

    private void updateMoveWithMap(Move move, int score) {

    }

}
