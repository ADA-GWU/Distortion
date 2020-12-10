# assigment-3-Natavan-A
assigment-3-Natavan-A created by GitHub Classroom

# Documentation

## Distortion

Distortion is a class that changes the given image by setting given block sizes to their average color.
It takes three arguments from the command line: file name, square size and the processing mode.
From left to right, top to bottom, it finds the average color for the (square size) x (square size) boxes
and sets the color of the whole square to this average color.
The class works in two modes: single-threaded and multi-threaded and shows the result by progress.
The result shall be saved in a result.jpg file.

@author  Natavan Akhundova

________________________________________________________________________________________________________

## `class Color`

Color is class for storing an RGB color of a pixel in three variables.

## `class Multi implements Runnable`

Multi is a class executing a task in parallel manner by implementing Runnable Interface.

## `Multi(int part)`

 * **Parameters:** `part` — indicates the ordering number of the thread

## `@Override public void run()`

Ensures parallel execution.

## `public class Distortion`

Has two methods: renderSingle and renderMulti.

 * **See also:** Distortion class description above

## `public static void renderSingle(int part) throws IOException`

Processes each block of the given image and sets the block to its average color in a single-threaded mode.

 * **Parameters:** `part` — indicates which part of divided segments of the image to process

     if -1 -> means single-threaded processing; the image will not be divided into segments,

     other value -> means multi-threaded processing; the image will be divided into segments.
 
 * **Exceptions:** `IOException` — to be handled in the main method.

## `public static void renderMulti() throws IOException, InterruptedException`

Processes each block of the given image and sets the block to its average color in a multi-threaded mode.

 * **Exceptions:** `IOException` — to be handled in the main method.

## `public static void main(String[] args)`

Processes user-submitted parameters and starts the execution according to them.

 * **Parameters:** `args` — accepts String type name of a file, integer type block size

     and String type case-insensitive processing mode - either "S" or "M".
