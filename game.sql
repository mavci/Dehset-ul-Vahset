CREATE TABLE IF NOT EXISTS `users` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `phoneKey` varchar(100) COLLATE utf8_turkish_ci NOT NULL,
  `name` varchar(32) COLLATE utf8_turkish_ci NOT NULL,
  `secret` varchar(16) COLLATE utf8_turkish_ci NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COLLATE=utf8_turkish_ci AUTO_INCREMENT=1 ;

CREATE TABLE IF NOT EXISTS `user_datas` (
  `userID` int(11) NOT NULL,
  `x` int(11) NOT NULL DEFAULT '0',
  `y` int(11) NOT NULL DEFAULT '0',
  `cX` int(11) NOT NULL DEFAULT '0',
  `cY` int(11) NOT NULL DEFAULT '0',
  `health` int(11) NOT NULL DEFAULT '100',
  `maxHealth` int(11) NOT NULL DEFAULT '100',
  `power` int(11) NOT NULL DEFAULT '10',
  `money` int(11) NOT NULL DEFAULT '0',
  `speed` int(11) NOT NULL DEFAULT '1',
  `level` int(11) NOT NULL DEFAULT '1',
  `exp` int(11) NOT NULL DEFAULT '0',
  `gm` char(1) COLLATE utf8_turkish_ci NOT NULL DEFAULT '0',
  PRIMARY KEY (`userID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_turkish_ci;
