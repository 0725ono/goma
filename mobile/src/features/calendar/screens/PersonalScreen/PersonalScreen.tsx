import {
  ActivityIndicator,
  FlatList,
  Pressable,
  StyleSheet,
  Text,
  View,
} from "react-native";

import { useEvents } from "@/features/calendar/api/useEvents";
import { getErrorMessage } from "@/lib/api/getErrorMessage";
import type { Event } from "@/features/calendar/model/event";

/** API の UTC 文字列をローカルタイムの「M/D HH:mm」に整形する（この画面ローカルの表示都合） */
const formatRange = (event: Event) => {
  const start = new Date(event.startAt);
  const end = new Date(event.endAt);
  const date = `${start.getMonth() + 1}/${start.getDate()}`;
  const time = (d: Date) =>
    `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
  return `${date} ${time(start)}〜${time(end)}`;
};

const EventListItem = ({ event }: { event: Event }) => (
  <View style={styles.item}>
    <Text style={styles.itemTitle}>{event.title}</Text>
    <Text style={styles.itemTime}>{formatRange(event)}</Text>
  </View>
);

const EventList = () => {
  const { data, isPending, isError, error, refetch } = useEvents();

  if (isPending) {
    return (
      <View style={styles.centerBox}>
        <ActivityIndicator />
      </View>
    );
  }

  if (isError) {
    // 文言は必ず getErrorMessage を通す（error.message は英語の内部文言なので出さない）
    return (
      <View style={styles.centerBox}>
        <Text style={styles.errorText}>予定を取得できませんでした</Text>
        <Text style={styles.errorDetail}>{getErrorMessage(error)}</Text>
        <Pressable style={styles.retryButton} onPress={() => refetch()}>
          <Text style={styles.retryText}>再試行</Text>
        </Pressable>
      </View>
    );
  }

  if (data.length === 0) {
    return (
      <View style={styles.centerBox}>
        <Text style={styles.emptyText}>予定はまだありません</Text>
      </View>
    );
  }

  return (
    <FlatList
      data={data}
      keyExtractor={(event) => event.id}
      renderItem={({ item }) => <EventListItem event={item} />}
      contentContainerStyle={styles.list}
    />
  );
};

export default function PersonalScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.pageTitle}>個人スケジュール</Text>
      <EventList />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20, backgroundColor: "#F5F5F5" },
  pageTitle: {
    fontSize: 24,
    fontWeight: "bold",
    marginBottom: 20,
    color: "#333",
  },
  list: { gap: 10 },
  item: {
    backgroundColor: "#FFF",
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "#DDD",
    padding: 14,
  },
  itemTitle: { fontSize: 16, fontWeight: "bold", color: "#333" },
  itemTime: { marginTop: 4, fontSize: 13, color: "#666" },
  centerBox: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    gap: 6,
  },
  errorText: { fontSize: 15, fontWeight: "bold", color: "#D32F2F" },
  errorDetail: { fontSize: 13, color: "#666" },
  retryButton: {
    marginTop: 8,
    paddingHorizontal: 24,
    paddingVertical: 10,
    borderRadius: 8,
    backgroundColor: "#1976D2",
  },
  retryText: { color: "#FFF", fontSize: 14, fontWeight: "bold" },
  emptyText: { fontSize: 15, color: "#999" },
});
