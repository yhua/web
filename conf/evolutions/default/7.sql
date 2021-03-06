--
-- 表的结构 relationmap
--
# --- !Ups

CREATE TABLE relationmap (
  uid int(11) NOT NULL AUTO_INCREMENT,
  child_id varchar(40) NOT NULL,
  parent_id varchar(40) NOT NULL,
  card_num VARCHAR(20)  NOT NULL DEFAULT '',
  relationship VARCHAR(20)  NOT NULL,
  status       TINYINT      NOT NULL DEFAULT 1,
  UNIQUE (card_num),
  PRIMARY KEY (uid)
);

--
-- 转存表中的数据 relationmap
--

INSERT INTO relationmap (uid, child_id, parent_id, card_num, relationship) VALUES
(1, '1_1394545098158', '2_93740362_789', '0001234567', '妈妈'),
(2, '1_93740362_456', '2_93740362_456', '0001234568', '妈妈'),
(3, '1_93740362_9982', '2_93740362_790', '0001234569', '妈妈'),
(4, '1_93740362_778', '2_93740362_792', '0001234570', '妈妈'),
(5, '1_93740362_374', '2_93740362_888', '0001234580', '妈妈'),
(6, '1_93740362_374', '2_93740362_000', '0001234581', '舅舅');


# --- !Downs

DROP TABLE IF EXISTS relationmap;