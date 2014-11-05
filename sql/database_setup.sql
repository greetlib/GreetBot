-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

-- -----------------------------------------------------
-- Schema greet
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema greet
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `greet` DEFAULT CHARACTER SET latin1 ;
USE `greet` ;

-- -----------------------------------------------------
-- Table `greet`.`Tokens`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `greet`.`Tokens` (
  `TokenID` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `Token` VARCHAR(60) NOT NULL,
  `AccessLevel` TINYINT NOT NULL DEFAULT 1,
  `Email` VARCHAR(254) NOT NULL,
  `Created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`TokenID`),
  UNIQUE INDEX `TokenID_UNIQUE` (`TokenID` ASC),
  UNIQUE INDEX `Token_UNIQUE` (`Token` ASC),
  UNIQUE INDEX `Email_UNIQUE` (`Email` ASC))
ENGINE = InnoDB
DEFAULT CHARACTER SET = latin1;


-- -----------------------------------------------------
-- Table `greet`.`Networks`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `greet`.`Networks` (
  `NetworkID` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `Host` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`NetworkID`),
  UNIQUE INDEX `Host_UNIQUE` (`Host` ASC))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `greet`.`Channels`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `greet`.`Channels` (
  `ChannelID` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `ChannelName` VARCHAR(75) NOT NULL,
  `NetworkID` INT UNSIGNED NOT NULL,
  `TokenID` INT UNSIGNED NOT NULL,
  `Created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `CommandPrefix` VARCHAR(2) NULL DEFAULT '#!',
  PRIMARY KEY (`ChannelID`),
  UNIQUE INDEX `ChannelID_UNIQUE` (`ChannelID` ASC),
  INDEX `FK_Channels_Tokens_idx` (`TokenID` ASC),
  INDEX `FK_Channels_Networks_idx` (`NetworkID` ASC),
  CONSTRAINT `FK_Channels_Tokens`
    FOREIGN KEY (`TokenID`)
    REFERENCES `greet`.`Tokens` (`TokenID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_Channels_Networks`
    FOREIGN KEY (`NetworkID`)
    REFERENCES `greet`.`Networks` (`NetworkID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = latin1;


-- -----------------------------------------------------
-- Table `greet`.`ChannelAccess`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `greet`.`ChannelAccess` (
  `ChannelAccessID` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `TokenID` INT UNSIGNED NOT NULL,
  `ChannelID` INT UNSIGNED NOT NULL,
  `AccessLevel` TINYINT UNSIGNED NULL DEFAULT '1',
  `Created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `Modified` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`ChannelAccessID`),
  UNIQUE INDEX `id_UNIQUE` (`ChannelAccessID` ASC),
  INDEX `channel_idx` (`ChannelID` ASC),
  INDEX `token_idx` (`TokenID` ASC),
  CONSTRAINT `FK_ChannelAccess_Tokens`
    FOREIGN KEY (`TokenID`)
    REFERENCES `greet`.`Tokens` (`TokenID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_ChannelAccess_Channels`
    FOREIGN KEY (`ChannelID`)
    REFERENCES `greet`.`Channels` (`ChannelID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = latin1;


-- -----------------------------------------------------
-- Table `greet`.`Modules`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `greet`.`Modules` (
  `ModuleID` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `Name` VARCHAR(30) NOT NULL,
  `Author` VARCHAR(30) NULL DEFAULT NULL,
  `Version` VARCHAR(30) NOT NULL,
  `TokenID` INT UNSIGNED NULL,
  `Created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `Modified` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `ShortDescription` VARCHAR(100) NULL,
  `LongDescription` TEXT NULL,
  PRIMARY KEY (`ModuleID`),
  UNIQUE INDEX `Name_UNIQUE` (`Name` ASC),
  INDEX `FK_Modules_Tokens_idx` (`TokenID` ASC),
  UNIQUE INDEX `ModuleID_UNIQUE` (`ModuleID` ASC),
  CONSTRAINT `FK_Modules_Tokens`
    FOREIGN KEY (`TokenID`)
    REFERENCES `greet`.`Tokens` (`TokenID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = latin1;


-- -----------------------------------------------------
-- Table `greet`.`ChannelModules`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `greet`.`ChannelModules` (
  `ChannelModuleID` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `ChannelID` INT UNSIGNED NOT NULL,
  `ModuleID` INT UNSIGNED NOT NULL,
  PRIMARY KEY (`ChannelModuleID`),
  UNIQUE INDEX `id_UNIQUE` (`ChannelModuleID` ASC),
  INDEX `channel_idx` (`ChannelID` ASC),
  INDEX `module_idx` (`ModuleID` ASC),
  CONSTRAINT `FK_ChannelModules_Channels`
    FOREIGN KEY (`ChannelID`)
    REFERENCES `greet`.`Channels` (`ChannelID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_ChannelModules_Modules`
    FOREIGN KEY (`ModuleID`)
    REFERENCES `greet`.`Modules` (`ModuleID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = latin1;


-- -----------------------------------------------------
-- Table `greet`.`Hosts`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `greet`.`Hosts` (
  `HostID` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `TokenID` INT UNSIGNED NOT NULL,
  `Host` VARCHAR(80) NULL DEFAULT NULL,
  `Created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`HostID`),
  UNIQUE INDEX `HostID_UNIQUE` (`HostID` ASC),
  UNIQUE INDEX `Host_UNIQUE` (`Host` ASC),
  INDEX `FK_Hosts_Tokens_idx` (`TokenID` ASC),
  CONSTRAINT `FK_Hosts_Tokens`
    FOREIGN KEY (`TokenID`)
    REFERENCES `greet`.`Tokens` (`TokenID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = latin1;


-- -----------------------------------------------------
-- Table `greet`.`DefaultModuleData`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `greet`.`DefaultModuleData` (
  `DefaultModuleDataID` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `ModuleID` INT UNSIGNED NOT NULL,
  `Key` VARCHAR(15) NOT NULL,
  `Value` BLOB NULL,
  `Created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `Modified` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`DefaultModuleDataID`),
  INDEX `FK_ModuleData_Modules_idx` (`ModuleID` ASC),
  CONSTRAINT `FK_ModuleData_Modules`
    FOREIGN KEY (`ModuleID`)
    REFERENCES `greet`.`Modules` (`ModuleID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `greet`.`ChannelModuleData`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `greet`.`ChannelModuleData` (
  `ChannelModuleDataID` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `ModuleID` INT UNSIGNED NOT NULL,
  `DefaultModuleDataID` INT UNSIGNED NOT NULL,
  `ChannelID` INT UNSIGNED NOT NULL,
  `Key` VARCHAR(15) NOT NULL,
  `Value` BLOB NULL,
  `Created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `Modified` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`ChannelModuleDataID`),
  UNIQUE INDEX `ChannelModuleDataID_UNIQUE` (`ChannelModuleDataID` ASC),
  INDEX `FK_ChannelModuleData_Channels_idx` (`ChannelID` ASC),
  INDEX `FK_ChannelModuleData_Modules_idx` (`ModuleID` ASC),
  INDEX `FK_ChannelModuleData_DefaultModuleData_idx` (`DefaultModuleDataID` ASC),
  CONSTRAINT `FK_ChannelModuleData_Channels`
    FOREIGN KEY (`ChannelID`)
    REFERENCES `greet`.`Channels` (`ChannelID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_ChannelModuleData_Modules`
    FOREIGN KEY (`ModuleID`)
    REFERENCES `greet`.`Modules` (`ModuleID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_ChannelModuleData_DefaultModuleData`
    FOREIGN KEY (`DefaultModuleDataID`)
    REFERENCES `greet`.`DefaultModuleData` (`DefaultModuleDataID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `greet`.`Commands`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `greet`.`Commands` (
  `CommandID` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `ModuleID` INT UNSIGNED NOT NULL,
  `Name` VARCHAR(20) NOT NULL,
  `Help` VARCHAR(255) NULL,
  `Usage` VARCHAR(255) NULL,
  `AccessLevel` TINYINT NULL DEFAULT 1,
  `Enabled` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`CommandID`),
  UNIQUE INDEX `CommandID_UNIQUE` (`CommandID` ASC),
  UNIQUE INDEX `Name_UNIQUE` (`Name` ASC),
  INDEX `FK_Commands_Modules_idx` (`ModuleID` ASC),
  CONSTRAINT `FK_Commands_Modules`
    FOREIGN KEY (`ModuleID`)
    REFERENCES `greet`.`Modules` (`ModuleID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
