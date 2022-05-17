//package controllers;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//import controllers.pubsubframework.PubSubUtils;
//import java.util.Arrays;
//import java.util.concurrent.CopyOnWriteArrayList;
//import org.junit.jupiter.api.Test;
//
//
//class PubSubUtilsTest {
//  @Test
//  public void testGetClosestOffset() {
//    CopyOnWriteArrayList<Integer> offsetIndex = new CopyOnWriteArrayList<>(
//        Arrays.asList(0, 10, 25, 30));
//    int closestOffset = PubSubUtils.getClosestOffset(offsetIndex, 11);
//    assertEquals(10, closestOffset);
//  }
//
//  @Test
//  public void testGetClosestOffsetBiggerThanList() {
//    CopyOnWriteArrayList<Integer> offsetIndex = new CopyOnWriteArrayList<>(
//        Arrays.asList(0, 10, 25, 30));
//    int closestOffset = PubSubUtils.getClosestOffset(offsetIndex, 31);
//    assertEquals(30, closestOffset);
//  }
//
//  @Test
//  public void testGetClosestOffsetZero() {
//    CopyOnWriteArrayList<Integer> offsetIndex = new CopyOnWriteArrayList<>(
//        Arrays.asList(0, 10, 25, 30));
//    int closestOffset = PubSubUtils.getClosestOffset(offsetIndex, 0);
//    assertEquals(0, closestOffset);
//  }
//
//  @Test
//  public void testGetClosestOffsetNegative() {
//    CopyOnWriteArrayList<Integer> offsetIndex = new CopyOnWriteArrayList<>(
//        Arrays.asList(0, 10, 25, 30));
//    int closestOffset = PubSubUtils.getClosestOffset(offsetIndex, -1);
//    assertEquals(0, closestOffset);
//  }
//
//  @Test
//  public void testGetByteToSkip() {
//    CopyOnWriteArrayList<Integer> offsetIndex = new CopyOnWriteArrayList<>(
//        Arrays.asList(0, 10, 25, 30));
//    int byteToSkip = PubSubUtils.getByteToSkip(offsetIndex, 25);
//    assertEquals(34, byteToSkip );
//  }
//
//  @Test
//  public void testGetByteToSkipZero() {
//    CopyOnWriteArrayList<Integer> offsetIndex = new CopyOnWriteArrayList<>(
//        Arrays.asList(0, 10, 25, 30));
//    int byteToSkip = PubSubUtils.getByteToSkip(offsetIndex, 0);
//    assertEquals(0, byteToSkip );
//  }
//}