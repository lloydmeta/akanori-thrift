package com.beachape.actors

import com.beachape.analyze.MorphemesRedisRetriever

/**
 * Message case classes to make passing messages
 *  between actors safer and easier
 */

// Common types
sealed case class RedisKey(redisKey: String)
sealed case class RedisKeySet(expectedKey: RedisKey, observedKey: RedisKey)

sealed case class UnixTime(time: Int)
sealed case class UnixTimeSpan(start: UnixTime, end: UnixTime)

sealed case class StoreString(stringToStore: String, unixCreatedAtTime: Int, weeksAgoDataToExpire: Int)

//Main Orchestrator Messages
sealed case class GenerateDefaultTrends
sealed case class GetDefaultTrends
sealed case class FetchTrendsEndingAt(
  unixEndAtTime: Int,
  spanInSeconds: Int,
  minOccurrence: Double,
  minLength: Int,
  maxLength: Int,
  top: Int,
  dropBlacklisted: Boolean,
  onlyWhitelisted: Boolean)

// Worker messages
sealed case class GenerateAndCacheTrendsFor(
  redisCacheKey: RedisKey,
  unixEndAtTime: Int,
  spanInSeconds: Int,
  minOccurrence: Double,
  minLength: Int,
  maxLength: Int,
  top: Int,
  dropBlacklisted: Boolean,
  onlyWhitelisted: Boolean)

sealed case class GenerateMorphemesFor(
  unixEndAtTime: Int,
  spanInSeconds: Int,
  dropBlacklisted: Boolean,
  onlyWhitelisted: Boolean)

sealed case class GenerateMorphemesForSpan(
  unixTimeSpan: UnixTimeSpan,
  dropBlacklisted: Boolean,
  onlyWhitelisted: Boolean)

sealed case class CalculateAndStoreTrendiness(
  term: String,
  newObservedScore: Double,
  trendsCacheKey: RedisKey,
  oldSetMorphemesRetriever: MorphemesRedisRetriever,
  newSetMorphemesRetriever: MorphemesRedisRetriever,
  oldSetObservedTotalScore: Double,
  oldSetExpectedTotalScore: Double,
  newSetObservedTotalScore: Double,
  newSetExpectedTotalScore: Double)

sealed case class RedisSetPair(oldSet: RedisKeySet, newSet: RedisKeySet)

sealed case class AnalyseAndStoreInRedisKey(
  phrase: String,
  redisKey: RedisKey,
  dropBlacklisted: Boolean,
  onlyWhitelisted: Boolean)