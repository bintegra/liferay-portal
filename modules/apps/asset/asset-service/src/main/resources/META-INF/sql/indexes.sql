create unique index IX_71A0231C on AssetEntryUsage (assetEntryId, plid, containerType, containerKey[$COLUMN_LENGTH:200$]);
create index IX_2816A7BF on AssetEntryUsage (assetEntryId, type_);
create index IX_8DA9ACD on AssetEntryUsage (plid, containerType, containerKey[$COLUMN_LENGTH:200$]);
create unique index IX_D0F18ABB on AssetEntryUsage (uuid_[$COLUMN_LENGTH:75$], groupId);