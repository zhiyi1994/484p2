package project2;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.TreeSet;
import java.util.Vector;
import java.sql.PreparedStatement;

public class MyFakebookOracle extends FakebookOracle {
	
	static String prefix = "ethanjyx.";
	
	// You must use the following variable as the JDBC connection
	Connection oracleConnection = null;
	
	// You must refer to the following variables for the corresponding tables in your database
	String cityTableName = null;
	String userTableName = null;
	String friendsTableName = null;
	String currentCityTableName = null;
	String hometownCityTableName = null;
	String programTableName = null;
	String educationTableName = null;
	String eventTableName = null;
	String participantTableName = null;
	String albumTableName = null;
	String photoTableName = null;
	String coverPhotoTableName = null;
	String tagTableName = null;
	
	
	// DO NOT modify this constructor
	public MyFakebookOracle(String u, Connection c) {
		super();
		String dataType = u;
		oracleConnection = c;
		// You will use the following tables in your Java code
		cityTableName = prefix+dataType+"_CITIES";
		userTableName = prefix+dataType+"_USERS";
		friendsTableName = prefix+dataType+"_FRIENDS";
		currentCityTableName = prefix+dataType+"_USER_CURRENT_CITY";
		hometownCityTableName = prefix+dataType+"_USER_HOMETOWN_CITY";
		programTableName = prefix+dataType+"_PROGRAMS";
		educationTableName = prefix+dataType+"_EDUCATION";
		eventTableName = prefix+dataType+"_USER_EVENTS";
		albumTableName = prefix+dataType+"_ALBUMS";
		photoTableName = prefix+dataType+"_PHOTOS";
		tagTableName = prefix+dataType+"_TAGS";
	}
	
	
	@Override
	// ***** Query 0 *****
	// This query is given to your for free;
	// You can use it as an example to help you write your own code
	//
	public void findMonthOfBirthInfo() throws SQLException{ 
		ResultSet rst = null; 
		PreparedStatement getMonthCountStmt = null;
		PreparedStatement getNamesMostMonthStmt = null;
		PreparedStatement getNamesLeastMonthStmt = null;
		
		try {
			// Scrollable result set allows us to read forward (using next())
			// and also backward.  
			// This is needed here to support the user of isFirst() and isLast() methods,
			// but in many cases you will not need it.
			// To create a "normal" (unscrollable) statement, you would simply call
			// stmt = oracleConnection.prepareStatement(sql);
			//
			String getMonthCountSql = "select count(*), month_of_birth from " +
				userTableName +
				" where month_of_birth is not null group by month_of_birth order by 1 desc";
			getMonthCountStmt = oracleConnection.prepareStatement(getMonthCountSql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			
			// getMonthCountSql is the query that will run
			// For each month, find the number of friends born that month
			// Sort them in descending order of count
			// executeQuery will run the query and generate the result set
			rst = getMonthCountStmt.executeQuery();
			
			this.monthOfMostFriend = 0;
			this.monthOfLeastFriend = 0;
			this.totalFriendsWithMonthOfBirth = 0;
			while(rst.next()) {
				int count = rst.getInt(1);
				int month = rst.getInt(2);
				if (rst.isFirst())
					this.monthOfMostFriend = month;
				if (rst.isLast())
					this.monthOfLeastFriend = month;
				this.totalFriendsWithMonthOfBirth += count;
			}
			
			// Get the month with most friends, and the month with least friends.
			// (Notice that this only considers months for which the number of friends is > 0)
			// Also, count how many total friends have listed month of birth (i.e., month_of_birth not null)
			//
			
			// Get the names of friends born in the "most" month
			String getNamesMostMonthSql = "select user_id, first_name, last_name from " + 
				userTableName + 
				" where month_of_birth = ?";
			getNamesMostMonthStmt = oracleConnection.prepareStatement(getNamesMostMonthSql);
			
			// set the first ? in the sql above to value this.monthOfMostFriend, with Integer type
			getNamesMostMonthStmt.setInt(1, this.monthOfMostFriend);
			rst = getNamesMostMonthStmt.executeQuery();
			while(rst.next()) {
				Long uid = rst.getLong(1);
				String firstName = rst.getString(2);
				String lastName = rst.getString(3);
				this.friendsInMonthOfMost.add(new UserInfo(uid, firstName, lastName));
			}
			
			// Get the names of friends born in the "least" month
			String getNamesLeastMonthSql = "select first_name, last_name, user_id from " + 
				userTableName + 
				" where month_of_birth = ?";
			getNamesLeastMonthStmt = oracleConnection.prepareStatement(getNamesLeastMonthSql);
			getNamesLeastMonthStmt.setInt(1, this.monthOfLeastFriend);
			
			rst = getNamesLeastMonthStmt.executeQuery();
			while(rst.next()){
				String firstName = rst.getString(1);
				String lastName = rst.getString(2);
				Long uid = rst.getLong(3);
				this.friendsInMonthOfLeast.add(new UserInfo(uid, firstName, lastName));
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
			// can do more things here
			
			throw e;		
		} finally {
			// Close statement and result set
			if(rst != null) 
				rst.close();
			
			if(getMonthCountStmt != null)
				getMonthCountStmt.close();
			
			if(getNamesMostMonthStmt != null)
				getNamesMostMonthStmt.close();
			
			if(getNamesLeastMonthStmt != null)
				getNamesLeastMonthStmt.close();
		}
	}

	
	
	@Override
	// ***** Query 1 *****
	// Find information about friend names:
	// (1) The longest last name (if there is a tie, include all in result)
	// (2) The shortest last name (if there is a tie, include all in result)
	// (3) The most common last name, and the number of times it appears (if there is a tie, include all in result)
	//
	public void findNameInfo() throws SQLException { // Query1
		
		ResultSet rst = null; 
		PreparedStatement getLongestLastnameStmt = null;
		PreparedStatement getShortestLastnameStmt = null;
		PreparedStatement getMostCommonnameStmt = null;		
		
		try{
			String getMostCommonnamesql = "SELECT DISTINCT U1.LAST_NAME, COUNT(*) FROM "
											+ userTableName + " U1 " 
											+"GROUP BY U1.LAST_NAME "
											+"HAVING COUNT(*) = (SELECT MAX(T.NAME_NUM) FROM (SELECT  DISTINCT COUNT(*) AS NAME_NUM, U2.LAST_NAME FROM USERS U2 GROUP BY U2.LAST_NAME) T)";
			
			getMostCommonnameStmt = oracleConnection.prepareStatement(getMostCommonnamesql);
			rst = getMostCommonnameStmt.executeQuery();
			while(rst.next()){
				this.mostCommonLastNames.add(rst.getString(1));	
				this.mostCommonLastNamesCount = rst.getInt(2);				
			}
			

			String getLongestLastnamesql = "SELECT DISTINCT U1.LAST_NAME FROM " 
											+ userTableName + " U1 " 
											+"WHERE LENGTH(U1.LAST_NAME) = (SELECT MAX(LENGTH(LAST_NAME)) FROM USERS)";//  "; length(last_name), where last_name is not null  count(*), 
			getLongestLastnameStmt = oracleConnection.prepareStatement(getLongestLastnamesql);//, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rst = getLongestLastnameStmt.executeQuery();
			
			
			//int longestNameLength = Integer.MIN_VALUE, shortestNameLength = Integer.MAX_VALUE, mostCommonCount = Integer.MIN_VALUE;
			while(rst.next()){
					this.longestLastNames.add(rst.getString(1));

			}			
			
			String getShortestLastnamesql = "SELECT DISTINCT U1.LAST_NAME FROM " 
					+ userTableName + " U1 " 
					+"WHERE LENGTH(U1.LAST_NAME) = (SELECT MIN(LENGTH(LAST_NAME)) FROM USERS)";//  "; length(last_name), where last_name is not null  count(*), 
			getShortestLastnameStmt = oracleConnection.prepareStatement(getShortestLastnamesql);//, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rst = getShortestLastnameStmt.executeQuery();

				while(rst.next()){
					this.shortestLastNames.add(rst.getString(1));

				}	
			
			
		}
		catch(SQLException e){
			System.err.println(e.getMessage());
		}
		finally{
			if(rst != null) 
				rst.close();
			
			if(getLongestLastnameStmt != null)
				getLongestLastnameStmt.close();
			
			if(getShortestLastnameStmt != null)
				getShortestLastnameStmt.close();
			
			if(getMostCommonnameStmt != null)
				getMostCommonnameStmt.close();			
		}
		
		
	}
	
	@Override
	// ***** Query 2 *****
	// Find the user(s) who have strictly more than 80 friends in the network
	//
	// Be careful on this query!
	// Remember that if two users are friends, the friends table
	// only contains the pair of user ids once, subject to 
	// the constraint that user1_id < user2_id
	//
	public void popularFriends() throws SQLException {
		// Find the following information from your database and store the information as shown 
		this.popularFriends.add(new UserInfo(10L, "Billy", "SmellsFunny"));
		this.popularFriends.add(new UserInfo(11L, "Jenny", "BadBreath"));
		this.countPopularFriends = 2;
	}
	 

	@Override
	// ***** Query 3 *****
	// Find the users who still live in their hometowns
	// (I.e., current_city = hometown_city)
	//	
	public void liveAtHome() throws SQLException {
		this.countLiveAtHome = 0;
		
		ResultSet rst = null; 
		PreparedStatement homeBuddyStmt = null;		
		
		try{
			String homeBuddysql = "select U.user_id, U.first_name, U.last_name from " + userTableName + " U, " + currentCityTableName + " C, "
					+ hometownCityTableName + " H where H.user_id = U.user_id and C.user_id = U.user_id and C.current_city_id = H.hometown_city_id";
			homeBuddyStmt = oracleConnection.prepareStatement(homeBuddysql);
			rst = homeBuddyStmt.executeQuery();
			while(rst.next()){
				Long id = rst.getLong(1);
				String first = rst.getString(2), last = rst.getString(3);	
				this.liveAtHome.add(new UserInfo(id, first, last));
				++this.countLiveAtHome;
			}
			
			
		}
		catch(SQLException e){
			System.err.println(e.getMessage());
		}
		finally{
			if(rst != null) 
				rst.close();
			
			if(homeBuddyStmt != null)
				homeBuddyStmt.close();			
		}
		
	}



	@Override
	// **** Query 4 ****
	// Find the top-n photos based on the number of tagged users
	// If there are ties, choose the photo with the smaller numeric PhotoID first
	// 
	public void findPhotosWithMostTags(int n) throws SQLException { 
		String photoId = "1234567";
		String albumId = "123456789";
		String albumName = "album1";
		String photoCaption = "caption1";
		String photoLink = "http://google.com";
		PhotoInfo p = new PhotoInfo(photoId, albumId, albumName, photoCaption, photoLink);
		TaggedPhotoInfo tp = new TaggedPhotoInfo(p);
		tp.addTaggedUser(new UserInfo(12345L, "taggedUserFirstName1", "taggedUserLastName1"));
		tp.addTaggedUser(new UserInfo(12345L, "taggedUserFirstName2", "taggedUserLastName2"));
		this.photosWithMostTags.add(tp);
	}

	
	
	
	@Override
	// **** Query 5 ****
	// Find suggested "match pairs" of friends, using the following criteria:
	// (1) One of the friends is female, and the other is male
	// (2) Their age difference is within "yearDiff"
	// (3) They are not friends with one another
	// (4) They should be tagged together in at least one photo
	//
	// You should up to n "match pairs"
	// If there are more than n match pairs, you should break ties as follows:
	// (i) First choose the pairs with the largest number of shared photos
	// (ii) If there are still ties, choose the pair with the smaller user_id for the female
	// (iii) If there are still ties, choose the pair with the smaller user_id for the male
	//
	public void matchMaker(int n, int yearDiff) throws SQLException { 		
		PreparedStatement matchPair_sharedPhotoStmt = null;
		ResultSet rst = null; 
		
	
		try{
			String matchPair_sharedPhotosql = "SELECT DISTINCT PA.NUM, F.USER_ID, F.FIRST_NAME, F.LAST_NAME, F.YEAR_OF_BIRTH, M.USER_ID, M.FIRST_NAME, M.LAST_NAME, M.YEAR_OF_BIRTH, PH.PHOTO_ID, PH.ALBUM_ID, AL.ALBUM_NAME, PH.PHOTO_CAPTION, PH.PHOTO_LINK "
												+ "FROM (SELECT DISTINCT M.USER_ID AS M_ID, F.USER_ID AS F_ID, COUNT (*) AS NUM FROM " 
														+ userTableName + " M, " + userTableName + " F, " + tagTableName + " TM, " + tagTableName + " TF " 
														+ "WHERE M.GENDER = 'male' AND F.GENDER = 'female' AND ABS(M.YEAR_OF_BIRTH - F.YEAR_OF_BIRTH) <= 2 AND TM.TAG_SUBJECT_ID = M.USER_ID AND TF.TAG_SUBJECT_ID = F.USER_ID " 
															+ "AND TM.TAG_PHOTO_ID = TF.TAG_PHOTO_ID AND ROWNUM <= 5 AND NOT EXISTS(SELECT * FROM "+
																friendsTableName + " F1 WHERE (F1.USER1_ID = M.USER_ID AND F1.USER2_ID = F.USER_ID) OR (F1.USER2_ID = M.USER_ID AND F1.USER1_ID = F.USER_ID)) " 
															+ "GROUP BY M.USER_ID, F.USER_ID HAVING COUNT (*) > 0 ORDER BY NUM DESC, F.USER_ID ASC, M.USER_ID ASC) PA, "
												+ userTableName + " M, " + userTableName + " F, " + tagTableName + " TM, " + tagTableName + " TF, " + photoTableName + " PH, " + albumTableName +" AL "
												+"WHERE M.USER_ID = PA.M_ID AND F.USER_ID = PA.F_ID AND TM.TAG_SUBJECT_ID = M.USER_ID AND TF.TAG_SUBJECT_ID = F.USER_ID AND TM.TAG_PHOTO_ID = TF.TAG_PHOTO_ID AND PH.PHOTO_ID = TM.TAG_PHOTO_ID AND PH.ALBUM_ID = AL.ALBUM_ID "
												+"ORDER BY F.USER_ID ASC, M.USER_ID ASC";
												 
			
			matchPair_sharedPhotoStmt = oracleConnection.prepareStatement(matchPair_sharedPhotosql);
			rst = matchPair_sharedPhotoStmt.executeQuery();	
			while(rst.next()){
				int num_ph = rst.getInt(1);
				MatchPair mp = new MatchPair(rst.getLong(2), rst.getString(3), rst.getString(4), rst.getInt(5), rst.getLong(6), rst.getString(7), rst.getString(8), rst.getInt(9));
				mp.addSharedPhoto(new PhotoInfo(rst.getString(10), rst.getString(11), rst.getString(12), rst.getString(13), rst.getString(14)));
				for(int i = 2; i <= num_ph; ++i){
					rst.next();
					mp.addSharedPhoto(new PhotoInfo(rst.getString(10), rst.getString(11), rst.getString(12), rst.getString(13), rst.getString(14)));
				}
				this.bestMatches.add(mp);				
			}
			
		}
		catch(SQLException e){
			System.err.println(e.getMessage());
		}		
		finally{
			if(rst != null) 
				rst.close();
			
			if(matchPair_sharedPhotoStmt != null) 
				matchPair_sharedPhotoStmt.close();			
		}				
	}

	
	// **** Query 6 ****
	// Suggest friends based on mutual friends
	// 
	// Find the top n pairs of users in the database who share the most
	// friends, but such that the two users are not friends themselves.
	//
	// Your output will consist of a set of pairs (user1_id, user2_id)
	// No pair should appear in the result twice; you should always order the pairs so that
	// user1_id < user2_id
	//
	// If there are ties, you should give priority to the pair with the smaller user1_id.
	// If there are still ties, give priority to the pair with the smaller user2_id.
	//
	@Override
	public void suggestFriendsByMutualFriends(int n) throws SQLException {
		Long user1_id = 123L;
		String user1FirstName = "Friend1FirstName";
		String user1LastName = "Friend1LastName";
		Long user2_id = 456L;
		String user2FirstName = "Friend2FirstName";
		String user2LastName = "Friend2LastName";
		FriendsPair p = new FriendsPair(user1_id, user1FirstName, user1LastName, user2_id, user2FirstName, user2LastName);

		p.addSharedFriend(567L, "sharedFriend1FirstName", "sharedFriend1LastName");
		p.addSharedFriend(678L, "sharedFriend2FirstName", "sharedFriend2LastName");
		p.addSharedFriend(789L, "sharedFriend3FirstName", "sharedFriend3LastName");
		this.suggestedFriendsPairs.add(p);
	}
	
	
	//@Override
	// ***** Query 7 *****
	// Given the ID of a user, find information about that
	// user's oldest friend and youngest friend
	// 
	// If two users have exactly the same age, meaning that they were born
	// on the same day, then assume that the one with the larger user_id is older
	//
	public void findAgeInfo(Long user_id) throws SQLException {
		//this.oldestFriend = new UserInfo(1L, "Oliver", "Oldham");
		//this.youngestFriend = new UserInfo(25L, "Yolanda", "Young");
		Long largeid = Long.MIN_VALUE;
		Long minid = Long.MAX_VALUE;
		
		ResultSet rst1 = null; 
		ResultSet rst2 = null; 
		PreparedStatement oldestStmt = null;	
		PreparedStatement youngestStmt = null;	
		try{
			
			String oldestsql = "SELECT DISTINCT U3.USER_ID, U3.FIRST_NAME, U3.LAST_NAME FROM (SELECT U2.USER_ID, U2.FIRST_NAME, U2.LAST_NAME, U2.YEAR_OF_BIRTH, U2.MONTH_OF_BIRTH, U2.DAY_OF_BIRTH "
					+ "FROM " + userTableName + " U2, " + userTableName + " U1, " + friendsTableName + " F " +
					"WHERE U1.USER_ID = " + user_id.toString() +" AND ((F.USER1_ID = U1.USER_ID AND F.USER2_ID = U2.USER_ID) OR (F.USER2_ID = U1.USER_ID AND F.USER1_ID = U2.USER_ID))) U3 " + 
					"WHERE 10000*U3.YEAR_OF_BIRTH + 100*U3.MONTH_OF_BIRTH + U3.DAY_OF_BIRTH = (SELECT MIN(10000*YEAR_OF_BIRTH + 100*MONTH_OF_BIRTH + DAY_OF_BIRTH) FROM (SELECT U2.USER_ID, U2.FIRST_NAME, U2.LAST_NAME, U2.YEAR_OF_BIRTH, U2.MONTH_OF_BIRTH, U2.DAY_OF_BIRTH "
					+ "FROM " + userTableName + " U2, " + userTableName + " U1, " + friendsTableName + " F " +
					"WHERE U1.USER_ID = " + user_id.toString() +" AND ((F.USER1_ID = U1.USER_ID AND F.USER2_ID = U2.USER_ID) OR (F.USER2_ID = U1.USER_ID AND F.USER1_ID = U2.USER_ID))))";
			oldestStmt = oracleConnection.prepareStatement(oldestsql);
			rst1 = oldestStmt.executeQuery();			
			while(rst1.next()){
				this.oldestFriend = new UserInfo(rst1.getLong(1), rst1.getString(2), rst1.getString(3));	
			}
			
			
			String youngestsql = "SELECT DISTINCT U3.USER_ID, U3.FIRST_NAME, U3.LAST_NAME FROM (SELECT U2.USER_ID, U2.FIRST_NAME, U2.LAST_NAME, U2.YEAR_OF_BIRTH, U2.MONTH_OF_BIRTH, U2.DAY_OF_BIRTH "
					+ "FROM " + userTableName + " U2, " + userTableName + " U1, " + friendsTableName + " F " +
					"WHERE U1.USER_ID = " + user_id.toString() +" AND ((F.USER1_ID = U1.USER_ID AND F.USER2_ID = U2.USER_ID) OR (F.USER2_ID = U1.USER_ID AND F.USER1_ID = U2.USER_ID))) U3 " + 
					"WHERE 10000*U3.YEAR_OF_BIRTH + 100*U3.MONTH_OF_BIRTH + U3.DAY_OF_BIRTH = (SELECT MAX(10000*YEAR_OF_BIRTH + 100*MONTH_OF_BIRTH + DAY_OF_BIRTH) FROM (SELECT U2.USER_ID, U2.FIRST_NAME, U2.LAST_NAME, U2.YEAR_OF_BIRTH, U2.MONTH_OF_BIRTH, U2.DAY_OF_BIRTH "
					+ "FROM " + userTableName + " U2, " + userTableName + " U1, " + friendsTableName + " F " +
					"WHERE U1.USER_ID = " + user_id.toString() +" AND ((F.USER1_ID = U1.USER_ID AND F.USER2_ID = U2.USER_ID) OR (F.USER2_ID = U1.USER_ID AND F.USER1_ID = U2.USER_ID))))";
			youngestStmt = oracleConnection.prepareStatement(youngestsql);
			rst2 = youngestStmt.executeQuery();			
			while(rst2.next()){
				Long tmp_id = rst2.getLong(1);
				if(tmp_id < minid) this.youngestFriend = new UserInfo(rst2.getLong(1), rst2.getString(2), rst2.getString(3));		
			}
		}
		catch(SQLException e){
			System.err.println(e.getMessage());
		}
		finally{
			if(rst1 != null) 
				rst1.close();
			
			if(rst2 != null) 
				rst2.close();
			
			if(oldestStmt != null)
				oldestStmt.close();		
			
			if(youngestStmt != null)
				youngestStmt.close();	
		}
		
	}
	
	@Override
	// ***** Query 8 *****
	// 
	// Find the name of the city with the most events, as well as the number of 
	// events in that city.  If there is a tie, return the names of all of the (tied) cities.
	//
	public void findEventCities() throws SQLException {
		this.eventCount = 12;
		this.popularCityNames.add("Ann Arbor");
		this.popularCityNames.add("Ypsilanti");
	}
	
	
	
	@Override
//	 ***** Query 9 *****
	//
	// Find pairs of potential siblings and print them out in the following format:
	//   # pairs of siblings
	//   sibling1 lastname(id) and sibling2 lastname(id)
	//   siblingA lastname(id) and siblingB lastname(id)  etc.
	//
	// A pair of users are potential siblings if they have the same last name and hometown, if they are friends, and
	// if they are less than 10 years apart in age.  Pairs of siblings are returned with the lower user_id user first
	// on the line.  They are ordered based on the first user_id and in the event of a tie, the second user_id.
	//  
	//
	public void findPotentialSiblings() throws SQLException {
		PreparedStatement findPairsStmt = null;
		ResultSet rst = null; 
		
	
		try{
			String findPairssql = "SELECT DISTINCT U1.USER_ID, U1.FIRST_NAME, U1.LAST_NAME, U2.USER_ID, U2.FIRST_NAME, U2.LAST_NAME FROM "
									+ userTableName + " U1, " + userTableName + " U2, " + hometownCityTableName + " H1, "+ hometownCityTableName + " H2 "
									+ "WHERE U1.LAST_NAME = U2.LAST_NAME AND H1.USER_ID = U1.USER_ID AND H2.USER_ID = U2.USER_ID AND H1.HOMETOWN_CITY_ID = H2.HOMETOWN_CITY_ID AND U1.USER_ID < U2.USER_ID AND ABS(U1.YEAR_OF_BIRTH - U2.YEAR_OF_BIRTH) < 10 "
									+ "AND EXISTS(SELECT * FROM " + friendsTableName + " F WHERE F.USER1_ID = U1.USER_ID AND F.USER2_ID = U2.USER_ID) " 
									+ "ORDER BY U1.USER_ID ASC, U2.USER_ID ASC";
			
			findPairsStmt = oracleConnection.prepareStatement(findPairssql);
			rst = findPairsStmt.executeQuery();	
			while(rst.next()){
				SiblingInfo s = new SiblingInfo(rst.getLong(1), rst.getString(2), rst.getString(3), rst.getLong(4), rst.getString(5), rst.getString(6));
				this.siblings.add(s);
			}
			
		}
		catch(SQLException e){
			System.err.println(e.getMessage());
		}		
		finally{
			if(rst != null) 
				rst.close();
			
			if(findPairsStmt != null) 
				findPairsStmt.close();			
		}
	
	}
	
}
