namespace java trendServer.gen
namespace rb TrendServer.Gen

typedef i64 Timestamp

struct TrendResult {
	1: required string term;
	2: required double trendiness;
}

service TrendThriftServer {

    // Simply returns the current time.
	Timestamp time(),
	
	// Methods for retrieving Trends
	list<TrendResult> currentTrendsDefault(),
	list<TrendResult> currentTrends(1:i32 spanInSeconds, 2:double minOccurrence, 3:i32 minLength, 4:i32 maxLength, 5:i32 top, 6:bool dropBlacklisted, 7:bool onlyWhitelisted,),
	list<TrendResult> trendsEndingAt(1:i32 unixEndAtTime, 2:i32 spanInSeconds, 3:double minOccurrence, 4:i32 minLength, 5:i32 maxLength, 6:i32 top, 7:bool dropBlacklisted, 8:bool onlyWhitelisted,),

	// Method for extracting words from a string
	list<string> stringToWords(1:string stringToAnalyze),
	
	oneway void storeString(1:string stringToStore, 2:i32 unixCreatedAtTime, 3:i32 weeksAgoDataToExpire)
}