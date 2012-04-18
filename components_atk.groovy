import groovy.sql.Sql
import java.sql.Date;

long rId = 1232406

//set the Connection
def atk = Sql.newInstance("mydatabase", "me", "mypassword", "com.mysql.jdbc.Driver");

//Find all Tape components
def titleIds = []
def pattern = ~/^Tape /
atk.eachRow("SELECT * FROM ResourcesComponents WHERE parentResourceComponentId = " + rId){
	def m = pattern.matcher(it.title)
	if(m.find()){
		String boxNum = null
		def numPattern = ~/\d{1,3}/
		def numMatch = numPattern.matcher(it.title)
		if (numMatch.find()) {
			boxNum = numMatch.group()
		}

		if(boxNum.length() == 1) 
			boxNum = "ms_1960_b000" + boxNum
		else if(boxNum.length() == 2)
			boxNum = "ms_1960_b00" + boxNum
		else if(boxNum.length() == 3)
			boxNum = "ms_1960_b0" + boxNum
		
		//build the components	
		def type = "Preservation masters"
		makeComponent(it.resourceComponentId, atk, boxNum, type)
		type = "Use copies"
		makeComponent(it.resourceComponentId, atk, boxNum, type)
	}
}

void makeComponent(cId, atk, bNum, type){
	atk.execute("UPDATE ResourcesComponents SET hasChild = b'1' WHERE resourceComponentId = ?", [cId])
 	def nextId = getMax(atk)
	nextId++;
	println bNum
	java.util.Date date = new java.util.Date();
	java.sql.Date d = new java.sql.Date( date.getTime() );
	
	def extent;
	if (type.equals("Preservation masters"))
		extent = "computer files (wav)"
	else
		extent = "computer files (mp4)"
	
	def pid = getPid(atk);
	def pString = 'ref' + pid
	atk.execute("INSERT INTO ResourcesComponents (resourceComponentId, version, lastUpdated, created, "
		+ "lastUpdatedBy, createdBy, title, resourceLevel, "
		+ "sequenceNumber, hasChild, parentResourceComponentId, hasNotes, persistentId, extentNumber, extentType) "
		+ "VALUES($nextId, 0, '" + d + "', '" + d + "', "
		+ "'scripted_process', 'scripted_process', '" + type + "', 'otherlevel', "
		+ "2, b'0', $cId, b'0', '$pString', 2, '" + extent + "')"
	);
	
	
	atk.execute("UPDATE Resources SET nextPersistentId = $pid WHERE resourceId = 3484");
	//build the ArchDescriptionInstances
	makeInstance(nextId, atk, bNum, type)
}

void makeInstance(cId, atk, bNum, type){
	long l = getMaxInstance(atk)
	l++;
	atk.execute("INSERT INTO ArchDescriptionInstances(archDescriptionInstancesId, instanceDescriminator, instanceType, resourceComponentId, parentResourceId) "
		+ "VALUES($l, 'digital', 'Digital object', $cId, 3484)"
	)
	//Build the DAOs
	makeDao(atk, l, bNum, type)
}

void makeDao(atk, instanceId, bNum, type){
	long l = getMaxDao(atk)
	l++;
	java.util.Date date = new java.util.Date();
	java.sql.Date d = new java.sql.Date( date.getTime() );
	
	def cId
	if(type.equals("Preservation masters"))
		cId = bNum + ".wav"		
	else
		cId = bNum + ".mp4"

	//make the digital object
	atk.execute("INSERT INTO DigitalObjects(digitalObjectId, version, lastUpdated, created, "
	 	+ "lastUpdatedBy, createdBy, title, metsIdentifier, "
		+ "objectType, objectOrder, archDescriptionInstancesId, repositoryId,  "
		+ "eadDaoActuate, eadDaoShow, restrictionsApply) "
		+ "VALUES($l, 0, '" + d + "', '" + d + "', "
		+ "'scripted_process', 'scripted_process', '" + type + "', '" + cId + "', "
		+ "'sound recording', 0, $instanceId, 1, 'onRequest', 'new', b'0')"
		);
		
	//make track components	
	l2 = getMaxDao(atk)
	l2++;
	
	def track
	if(type.equals("Preservation masters")){
		tracka = bNum + "a.wav"
		trackb = bNum + "b.wav"
	}
		
	else{
		tracka = bNum + "a.mp4"
		trackb = bNum + "b.mp4"
	}
		
	atk.execute("INSERT INTO DigitalObjects(digitalObjectId, version, lastUpdated, created, "
	 	+ "lastUpdatedBy, createdBy, title, componentId, "
		+ "repositoryId, restrictionsApply, parentDigitalObjectId, objectOrder) "
		+ "VALUES($l2, 0, '" + d + "', '" + d + "', "
		+ "'scripted_process', 'scripted_process', 'Track A', '" + tracka + "', "
		+ "1, b'0', $l, 1)"
	);
	
	l2 = getMaxDao(atk)
	l2++;

	atk.execute("INSERT INTO DigitalObjects(digitalObjectId, version, lastUpdated, created, "
	 	+ "lastUpdatedBy, createdBy, title, componentId, "
		+ "repositoryId, restrictionsApply, parentDigitalObjectId, objectOrder) "
		+ "VALUES($l2, 0, '" + d + "', '" + d + "', "
		+ "'scripted_process', 'scripted_process', 'Track B', '" + trackb + "', "
		+ "1, b'0', $l, 2)"
	);
}

Long getMax(atk){
	Long l
	atk.eachRow("SELECT Max(resourceComponentId) as id FROM ResourcesComponents"){ l = it.id}
	return l
}

Long getMaxInstance(atk){
	Long l
	atk.eachRow("SELECT Max(archDescriptionInstancesId) as id FROM ArchDescriptionInstances"){ l = it.id}
	return l
}

Long getMaxDao(atk){
	Long l
	atk.eachRow("SELECT Max(digitalObjectId) as id FROM DigitalObjects"){ l = it.id}
	return l
}

int getPid(atk) {
	def i
	atk.eachRow("SELECT nextPersistentId FROM Resources WHERE resourceId = 3484"){ i = it.nextPersistentId }
	i++
	return i
}
