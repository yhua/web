# --- !Ups

CREATE TABLE accountinfo (
  uid int(11) NOT NULL AUTO_INCREMENT,
  accountid      varchar(16) NOT NULL,
  password varchar(32) NOT NULL,
  pushid   varchar(20) NOT NULL DEFAULT '',
  active  int NOT NULL DEFAULT 0,
  pwd_change_time bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (uid)
);

INSERT INTO accountinfo (uid, accountid, password, pushid, active, pwd_change_time) VALUES
(1, '13402815317', '3FDE6BB0541387E4EBDADF7C2FF31123', '952021056346783736', 1, 1386425935574),
(2, '13880498549', '', '', 0, 0),
(3, '13408654680', '96E79218965EB72C92A549DD5A330112', '925387477040814447', 1, 1386849160798),
(4, '13333333333', '5F4DCC3B5AA765D61D8327DEB882CF99', '0', 0, 0),
(5, '13279491366', '5F4DCC3B5AA765D61D8327DEB882CF99', '1048149352597220995', 1, 1386849160798);


# --- !Downs

DROP TABLE IF EXISTS accountinfo;